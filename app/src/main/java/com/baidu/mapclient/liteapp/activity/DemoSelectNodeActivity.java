package com.baidu.mapclient.liteapp.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.baidu.mapclient.liteapp.BNDemoFactory;
import com.baidu.mapclient.liteapp.R;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;

/**
 * Author: v_duanpeifeng
 * Time: 2020-05-21
 * Description:
 */
public class DemoSelectNodeActivity extends Activity implements View.OnClickListener {

    private EditText carNumText;
    private EditText startNodeText;
    private EditText endNodeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_node);

        carNumText = findViewById(R.id.car_num);
        startNodeText = findViewById(R.id.start_node);
        endNodeText = findViewById(R.id.end_node);
        findViewById(R.id.confirm).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String carNum = carNumText.getText().toString();
        if (!TextUtils.isEmpty(carNum)) {
            BaiduNaviManagerFactory.getCommonSettingManager().setCarNum(carNum);
        }

        String startNode = startNodeText.getText().toString();
        if (!TextUtils.isEmpty(startNode)) {
            BNDemoFactory.getInstance().setStartNode(this, startNode);
        }

        String endNode = endNodeText.getText().toString();
        if (!TextUtils.isEmpty(endNode)) {
            BNDemoFactory.getInstance().setEndNode(this, endNode);
        }
    }
}
