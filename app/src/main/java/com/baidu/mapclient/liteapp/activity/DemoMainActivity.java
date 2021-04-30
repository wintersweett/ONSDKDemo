/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.mapclient.liteapp.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mapclient.liteapp.BNDemoFactory;
import com.baidu.mapclient.liteapp.BNDemoUtils;
import com.baidu.mapclient.liteapp.ForegroundService;
import com.baidu.mapclient.liteapp.R;
import com.baidu.mapclient.liteapp.controlwindow.ControlBoardWindow;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviCommonParams;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DemoMainActivity extends Activity {

    private static final String[] authBaseArr = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int authBaseRequestCode = 1;

    private Button mNaviBtn = null;
    private Button mTruckBtn = null;
    private Button mMotorBtn = null;
    private Button mExternalBtn = null;
    private Button mDrivingBtn = null;
    private Button mOverlayBtn = null;
    private Button mCruiserBtn = null;
    private Button mAnalogBtn = null;
    private Button mSelectNodeBtn = null;
    private Button mGotoSettingsBtn = null;

    private BroadcastReceiver mReceiver;
    private int mPageType = BNDemoUtils.NORMAL;
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_START:
                    Toast.makeText(DemoMainActivity.this, "算路开始", Toast.LENGTH_SHORT).show();
                    ControlBoardWindow.getInstance().showControl("算路开始");
                    break;
                case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_SUCCESS:
                    Toast.makeText(DemoMainActivity.this, "算路成功", Toast.LENGTH_SHORT).show();
                    ControlBoardWindow.getInstance().showControl("算路成功");
                    // 躲避限行消息
                    Bundle infoBundle = (Bundle) msg.obj;
                    if (infoBundle != null) {
                        String info = infoBundle
                                .getString(BNaviCommonParams.BNRouteInfoKey.TRAFFIC_LIMIT_INFO);
                        Log.e("OnSdkDemo", "info = " + info);
                    }
                    break;
                case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_FAILED:
                    ControlBoardWindow.getInstance().showControl("算路失败");
                    Toast.makeText(DemoMainActivity.this.getApplicationContext(),
                            "算路失败", Toast.LENGTH_SHORT).show();
                    break;
                case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_TO_NAVI:
                    Toast.makeText(DemoMainActivity.this.getApplicationContext(),
                            "算路成功准备进入导航", Toast.LENGTH_SHORT).show();
                    ControlBoardWindow.getInstance().showControl("算路成功准备进入导航");
                    switch (mPageType) {
                        case BNDemoUtils.NORMAL:
                            BNDemoUtils.gotoNavi(DemoMainActivity.this);
                            break;
                        case BNDemoUtils.ANALOG:
                            BNDemoUtils.gotoAnalog(DemoMainActivity.this);
                            break;
                        case BNDemoUtils.EXTGPS:
                            BNDemoUtils.gotoExtGps(DemoMainActivity.this);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    // nothing
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 开启前台服务防止应用进入后台gps挂掉
        startService(new Intent(this, ForegroundService.class));

        initView();
        initListener();
        initPermission();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 若未授权则请求权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            } else {
                ControlBoardWindow.getInstance().showPopupWindow(DemoMainActivity.this);
                ControlBoardWindow.getInstance().showControl("初始化");
            }
        }
        initBroadCastReceiver();
    }

    private void initBroadCastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.navi.ready");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BNDemoFactory.getInstance().initCarInfo();
                BNDemoFactory.getInstance().initRoutePlanNode();
            }
        };
        registerReceiver(mReceiver, filter);
    }

    private void initPermission() {
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasBasePhoneAuth()) {
                requestPermissions(authBaseArr, authBaseRequestCode);
            }
        }
    }

    private void initView() {
        mNaviBtn = findViewById(R.id.naviBtn);
        mTruckBtn = findViewById(R.id.truckBtn);
        mMotorBtn = findViewById(R.id.motorBtn);
        mExternalBtn = findViewById(R.id.externalBtn);
        mAnalogBtn = findViewById(R.id.analogBtn);
        mOverlayBtn = findViewById(R.id.overlayBtn);
        mDrivingBtn = findViewById(R.id.drivingBtn);
        mCruiserBtn = findViewById(R.id.cruiserBtn);
        mGotoSettingsBtn = findViewById(R.id.gotoSettingsBtn);
        mSelectNodeBtn = findViewById(R.id.selectNodeBtn);
    }

    private void initListener() {
        if (mNaviBtn != null) {

            mNaviBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        mPageType = BNDemoUtils.NORMAL;
                        routePlanToNavi(null);
                    }
                }
            });
        }

        if (mTruckBtn != null) {
            mTruckBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        mPageType = BNDemoUtils.NORMAL;
                        Bundle bundle = new Bundle();
                        bundle.putInt(BNaviCommonParams.RoutePlanKey.VEHICLE_TYPE,
                                IBNRoutePlanManager.Vehicle.TRUCK);
                        bundle.putString(BNaviCommonParams.RoutePlanKey.ASSIGN_ROUTE, "");
                        routePlanToNavi(bundle);
                    }
                }
            });
        }

        if (mMotorBtn != null) {
            mMotorBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        mPageType = BNDemoUtils.NORMAL;
                        Bundle bundle = new Bundle();
                        bundle.putInt(BNaviCommonParams.RoutePlanKey.VEHICLE_TYPE,
                                IBNRoutePlanManager.Vehicle.MOTOR);
                        routePlanToNavi(bundle);
                    }
                }
            });
        }

        if (mExternalBtn != null) {
            mExternalBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        mPageType = BNDemoUtils.EXTGPS;
                        routePlanToNavi(null);
                    }
                }
            });
        }

        if (mAnalogBtn != null) {
            mAnalogBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        mPageType = BNDemoUtils.ANALOG;
                        Bundle bundle = new Bundle();
                        bundle.putInt(BNaviCommonParams.RoutePlanKey.VEHICLE_TYPE,
                                IBNRoutePlanManager.Vehicle.CAR);
                        routePlanToNavi(bundle);
                    }
                }
            });
        }

        if (mOverlayBtn != null) {
            mOverlayBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        BNDemoUtils.gotoDrawOverlay(DemoMainActivity.this);
                    }
                }
            });
        }

        if (mDrivingBtn != null) {
            mDrivingBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        BNDemoUtils.gotoDriving(DemoMainActivity.this);
                    }
                }
            });
        }

        if (mCruiserBtn != null) {
            mCruiserBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        BNDemoUtils.gotoCruiser(DemoMainActivity.this);
                    }
                }
            });
        }

        if (mGotoSettingsBtn != null) {
            mGotoSettingsBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        BNDemoUtils.gotoSettings(DemoMainActivity.this);
                    }
                }
            });
        }

        if (mSelectNodeBtn != null) {
            mSelectNodeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        BNDemoUtils.gotoSelectNode(DemoMainActivity.this);
                    }
                }
            });
        }
    }

    private void routePlanToNavi(final Bundle bundle) {
        List<BNRoutePlanNode> list = new ArrayList<>();
        list.add(BNDemoFactory.getInstance().getStartNode(this));
        list.add(BNDemoFactory.getInstance().getEndNode(this));

        // 关闭电子狗
        if (BaiduNaviManagerFactory.getCruiserManager().isCruiserStarted()) {
            BaiduNaviManagerFactory.getCruiserManager().stopCruise();
        }
        BaiduNaviManagerFactory.getRoutePlanManager().routePlanToNavi(
                list,
                IBNRoutePlanManager.RoutePlanPreference.ROUTE_PLAN_PREFERENCE_DEFAULT,
                bundle, handler);
    }

    private boolean hasBasePhoneAuth() {
        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager
                    .PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions,
                                           @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret != 0) {
                    Toast.makeText(DemoMainActivity.this.getApplicationContext(),
                            "缺少导航基本的权限!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopService(new Intent(this, ForegroundService.class));
    }
}
