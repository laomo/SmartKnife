###SmartKnife

Simplify from [Butter Knife](https://github.com/JakeWharton/butterknife)

###Usage

Use like `app/src/main/java/com/laomo/smartknife/MainActivity.java`:

```
public class MainActivity extends Activity implements OnClickListener, OnItemClickListener {

    @ViewInject(id = R.id.text, click = true)
    TextView textView;

    @ViewInject(id = R.id.list, itemClick = true)
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SmartKnife.bind(this);
        textView.setText("点击");
        listView.setAdapter(new SimpleAdapter(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(this, "ViewInject Successfully!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(this, "点击了条目" + position, Toast.LENGTH_LONG).show();
    }

}
```
###Proguard
```
#for smartknife
-keep class **$$ViewBinder { *; }
-dontwarn com.laomo.inject.**
-keepnames class * { @com.laomo.inject.ViewInject *;}
-keepclasseswithmembernames class * {
    @com.laomo.inject.* <fields>;
}
#for smartknife end
```
###Develop
Because I didn't find a way that can use both `javax annotation` and `android` package, I tried a stupid trick:

1. set module `smartknife` apply plugin: 'java'
2. copy `android.jar` to module `smartknife` libs

If you have some good idea, please tell me.
###License
```
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
