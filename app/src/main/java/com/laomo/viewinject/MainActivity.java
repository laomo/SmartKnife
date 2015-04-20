package com.laomo.viewinject;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.laomo.inject.SmartKnife;
import com.laomo.inject.ViewInject;

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
