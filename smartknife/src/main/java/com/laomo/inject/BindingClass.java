package com.laomo.inject;

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;

final class BindingClass {

    private String packageName;
    private String targetClassName;
    private String binderClassName;

    private Map<Integer, ViewBinding> idViewMap = new HashMap<>();

    public BindingClass(String packageName, String className) {
        this.packageName = packageName;
        this.targetClassName = className;
        this.binderClassName = className + SmartKnifeProcessor.SUFFIX;
    }

    public void putViewInfo(int id, ViewBinding viewBinding) {
        idViewMap.put(id, viewBinding);
    }

    public String getBinderClassFullName() {
        return packageName + "." + binderClassName;
    }

    private String getTargetClassName(){
        return targetClassName.replace("$", ".");
    }

    public String generateJavaCode() {

        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from SmartKnife. Do not modify!\n")
                .append("package ").append(packageName).append(";\n\n")

                .append("import android.view.View;\n")
                .append("import android.view.View.OnClickListener;\n")
                .append("import android.widget.AdapterView.OnItemClickListener;\n")
                .append("import ").append(SmartKnife.class.getPackage().getName()).append(".SmartKnife.Finder;\n")
                .append("import ").append(SmartKnife.class.getPackage().getName()).append(".SmartKnife.ViewBinder;\n\n")

                .append("public class ").append(binderClassName)
                .append("<T extends ").append(getTargetClassName()).append(">")
                .append(" implements ViewBinder<T> {\n\n");

        emitBindMethod(builder);
        builder.append('\n');
        emitUnbindMethod(builder);
        builder.append("}\n");
        return builder.toString();

    }


    private void emitBindMethod(StringBuilder builder) {
        builder.append("  @Override ")
                .append("public void bind(final Finder finder, final T target, Object source) {\n");
        //TODO if has parent
        //.append("    super.bind(finder, target, source);\n")
        //.append("    View view;\n");

        for (Integer key : idViewMap.keySet()) {
            ViewBinding viewBinding = idViewMap.get(key);
            builder.append("    target.").append(viewBinding.getName()).append(" = ");
            if (viewBinding.isRequired()) {
                builder.append("finder.findRequiredView(source, ");
            } else {
                builder.append("finder.findOptionalView(source, ");
            }
            builder.append(viewBinding.getId()).append(", \"")
                    .append(viewBinding.getName()).append("\");\n");
            if (viewBinding.isClick()) {
                builder.append("    target.").append(viewBinding.getName())
                        .append(".setOnClickListener(target);\n");
            }
            if (viewBinding.isItemClick()) {
                builder.append("    target.").append(viewBinding.getName())
                        .append(".setOnItemClickListener(target);\n");
            }
        }
        builder.append("  }\n");
    }

    private void emitUnbindMethod(StringBuilder builder) {
        builder.append("  @Override ")
                .append("public void unbind(T target) {\n");
        //TODO if has parent
        //.append("    super.unbind(target);\n")
        for (ViewBinding viewBinding : idViewMap.values()) {
            builder.append("    target.").append(viewBinding.getName()).append(" = null;\n");
        }
        builder.append("  }\n");
    }
}
