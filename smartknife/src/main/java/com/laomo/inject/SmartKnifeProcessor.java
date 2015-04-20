package com.laomo.inject;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

import static javax.lang.model.element.ElementKind.CLASS;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.NOTE;

public class SmartKnifeProcessor extends AbstractProcessor {

    public static final String SUFFIX = "$$ViewBinder";
    public static final String ANDROID_PREFIX = "android.";
    public static final String JAVA_PREFIX = "java.";
    static final String VIEW_TYPE = "android.view.View";
    static final String ADAPTER_VIEW_TYPE = "android.widget.AdapterView<?>";
    private static final String NULLABLE_ANNOTATION_NAME = "Nullable";

    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(ViewInject.class.getCanonicalName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

        Map<String, BindingClass> bindMap = findAndParseTargets(env);
        for (String key : bindMap.keySet()) {
            BindingClass bindingClass = bindMap.get(key);
            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                        bindingClass.getBinderClassFullName(),
                        bindingClass.getTypeElement());
                Writer writer = jfo.openWriter();
                writer.write(bindingClass.generateJavaCode());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                error(bindingClass.getTypeElement(), "Unable to write injector for type %s: %s",
                        bindingClass.getTypeElement(), e.getMessage());
            }

        }
        return true;
    }

    private Map<String, BindingClass> findAndParseTargets(RoundEnvironment env) {

        Map<String, BindingClass> bindMap = new HashMap<>();

        for (Element element : env.getElementsAnnotatedWith(ViewInject.class)) {
            boolean hasError = false;
            TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

            // Verify that the target type extends from View.
            TypeMirror elementType = element.asType();
            if (elementType.getKind() == TypeKind.TYPEVAR) {
                TypeVariable typeVariable = (TypeVariable) elementType;
                elementType = typeVariable.getUpperBound();
            }
            if (!isSubtypeOfType(elementType, VIEW_TYPE) && !isInterface(elementType)) {
                error(element, "@FindView fields must extend from View or be an interface. (%s.%s)",
                        enclosingElement.getQualifiedName(), element.getSimpleName());
                hasError = true;
            }

            // Verify common generated code restrictions.
            hasError |= isInaccessibleViaGeneratedCode(ViewInject.class, "fields", element);
            hasError |= isBindingInWrongPackage(ViewInject.class, element);

            if (hasError) {
                continue;
            }

            ViewInject viewInject = element.getAnnotation(ViewInject.class);
            int id = viewInject.id();
            boolean click = viewInject.click();
            boolean itemClick = viewInject.itemClick();
            boolean required = isRequiredInjection(element);


            VariableElement varElement = (VariableElement) element;
            String fqClassName = enclosingElement.getQualifiedName().toString();

            PackageElement packageElement = elementUtils.getPackageOf(enclosingElement);
            String packageName = packageElement.getQualifiedName().toString();

            String className = getClassName(enclosingElement, packageName);
            String fieldName = varElement.getSimpleName().toString();
            String fieldType = varElement.asType().toString();

            if (itemClick) {
                itemClick = isSubtypeOfType(varElement.asType(), ADAPTER_VIEW_TYPE);
            }
            BindingClass bindingClass = bindMap.get(fqClassName);
            if (bindingClass == null) {
                bindingClass = new BindingClass(packageName, className, enclosingElement);
                bindMap.put(fqClassName, bindingClass);
            }
            bindingClass.putViewInfo(id,
                    new ViewBinding(id, fieldName, fieldType, click, itemClick, required));
        }
        return bindMap;
    }

    private static String getClassName(TypeElement type, String packageName) {
        int packageLen = packageName.length() + 1;
        return type.getQualifiedName().toString().substring(packageLen)
                .replace('.', '$');
    }

    private boolean isInaccessibleViaGeneratedCode(Class<? extends Annotation> annotationClass,
                                                   String targetThing, Element element) {
        boolean hasError = false;
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();

        // Verify method modifiers.
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(PRIVATE) || modifiers.contains(STATIC)) {
            error(element, "@%s %s must not be private or static. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing type.
        if (enclosingElement.getKind() != CLASS) {
            error(enclosingElement, "@%s %s may only be contained in classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        // Verify containing class visibility is not private.
        if (enclosingElement.getModifiers().contains(PRIVATE)) {
            error(enclosingElement, "@%s %s may not be contained in private classes. (%s.%s)",
                    annotationClass.getSimpleName(), targetThing, enclosingElement.getQualifiedName(),
                    element.getSimpleName());
            hasError = true;
        }

        return hasError;
    }

    private boolean isBindingInWrongPackage(Class<? extends Annotation> annotationClass,
                                            Element element) {
        TypeElement enclosingElement = (TypeElement) element.getEnclosingElement();
        String qualifiedName = enclosingElement.getQualifiedName().toString();

        if (qualifiedName.startsWith(ANDROID_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Android framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }
        if (qualifiedName.startsWith(JAVA_PREFIX)) {
            error(element, "@%s-annotated class incorrectly in Java framework package. (%s)",
                    annotationClass.getSimpleName(), qualifiedName);
            return true;
        }

        return false;
    }

    private boolean isInterface(TypeMirror typeMirror) {
        return typeMirror instanceof DeclaredType && ((DeclaredType) typeMirror).asElement().getKind() == INTERFACE;
    }

    private boolean isSubtypeOfType(TypeMirror typeMirror, String otherType) {
        if (otherType.equals(typeMirror.toString())) {
            return true;
        }
        if (!(typeMirror instanceof DeclaredType)) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() > 0) {
            StringBuilder typeString = new StringBuilder(declaredType.asElement().toString());
            typeString.append('<');
            for (int i = 0; i < typeArguments.size(); i++) {
                if (i > 0) {
                    typeString.append(',');
                }
                typeString.append('?');
            }
            typeString.append('>');
            if (typeString.toString().equals(otherType)) {
                return true;
            }
        }
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        TypeMirror superType = typeElement.getSuperclass();
        if (isSubtypeOfType(superType, otherType)) {
            return true;
        }
        for (TypeMirror interfaceType : typeElement.getInterfaces()) {
            if (isSubtypeOfType(interfaceType, otherType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotationWithName(Element element, String simpleName) {
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            String annotationName = mirror.getAnnotationType().asElement().getSimpleName().toString();
            if (simpleName.equals(annotationName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRequiredInjection(Element element) {
        return !hasAnnotationWithName(element, NULLABLE_ANNOTATION_NAME);
    }

    private void error(Element element, String message, Object... args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }
        processingEnv.getMessager().printMessage(NOTE, message, element);
    }
}
