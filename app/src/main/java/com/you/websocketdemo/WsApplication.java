package com.you.websocketdemo;

import android.app.Application;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocketAdapter;

/**
 * Created by youxuan on 2017/6/11 0011.
 */

public class WsApplication extends Application {


    private static WsApplication instance;

    private WsApplication() {
    }


    public static WsApplication getInstance() {
      return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initAppStatusListener();
    }

    private void initAppStatusListener() {
        ForegroundCallbacks.init(this).addListener(new ForegroundCallbacks.Listener() {
            @Override
            public void onBecameForeground() {
                Log.d("WsManager", "onBecameForeground: 应用回到前台调用重连方法");
                WsManager.getInstance().reconnect();
            }

            @Override
            public void onBecameBackground() {

            }
        });
    }
}