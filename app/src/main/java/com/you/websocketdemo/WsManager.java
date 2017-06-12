package com.you.websocketdemo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by youxuan on 2017/6/11 0011.
 */

public class WsManager {
    private static WsManager sWsManager;
    private final String TAG = this.getClass().getSimpleName();

    /**
     * WebSocket的配置
     */

    private static final int FRAME_QUEUE_SIZE = 5;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final String DEF_TEST_URL = "测试服务器地址";
    private static final String DEF_RELEASE_URL = "正式地址";
    private static final String DEF_URL = BuildConfig.DEBUG ?DEF_TEST_URL : DEF_RELEASE_URL;
    private String url;

    private WsStatus mStatus;
    private WebSocket ws;
    private WsListener mListener;



    private WsManager(){

    }

    public static WsManager getInstance(){
        if (sWsManager ==null){
            synchronized (WsManager.class){
                if (sWsManager ==null){
                    sWsManager = new WsManager();
                }
            }
        }
        return sWsManager;
    }

    private void init(){

        try {

            /**
             * configUrl其实是缓存在本地的连接
             * 这个缓存本地连接地址是app启动的时候通过http请求服务器去获取的
             * 每次app启动的时候回拿到当前时间与缓存时间比较,超过6小时
             * 就再次去服务端获取新的连接地址更新本地缓存
             */
            String configUrl = "";
            url = TextUtils.isEmpty(configUrl) ?DEF_URL:configUrl;
            ws = new WebSocketFactory().createSocket(url,CONNECT_TIMEOUT)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(mListener = new WsListener())
                    .connectAsynchronously();  //异步连接
            setStatus(WsStatus.CONNECTING);
            Log.d(TAG, "init: 第一次连接");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 继承默认的监听空实现WebSocketAdapter,重写我们需要的方法
     * onTextMessage 收到文字信息
     * onConnected 连接成功
     * onConnectError 连接失败
     * onDisconnected 连接关闭
     */

    class WsListener extends WebSocketAdapter{
        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            Log.d(TAG, "onTextMessage: "+text);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            Log.d(TAG, "onConnected: 连接成功");
            setStatus(WsStatus.CONNECT_SUCCESS);
            cancelReconnect();//连接成功的时候取消重连,初始化连接次数
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            Log.d(TAG, "onConnectError: 连接错误");
            setStatus(WsStatus.CONNECT_FAIL);
            reconnect();//连接错误的时候调用重连方法

        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            Log.d(TAG, "onDisconnected: 断开连接");
            setStatus(WsStatus.CONNECT_FAIL);
            reconnect();//连接断开的时候调用重连方法
        }
    }

    private void setStatus(WsStatus status){
        this.mStatus = status;
    }
    private WsStatus getStatus(){
        return mStatus;
    }

    public void disconnect(){
        if (ws!=null){
            ws.disconnect();
        }
    }


    private Handler mHandler = new Handler();

    private int reconnectCount = 0 ;//重连次数
    private long minInterval = 3000;// 重连最小时间间隔
    private long maxInterval = 60000; //重连最大时间间隔


    public void reconnect(){
        if (!isNetConnect()){
            reconnectCount = 0;
            Log.d(TAG, "reconnect: 重连失败网络不可用");
            return;
        }

        if (ws!=null && !ws.isOpen() //当前连接断开了

                && getStatus()!=WsStatus.CONNECTING){//不是正在连接状态
            reconnectCount ++;
            setStatus(WsStatus.CONNECTING);

            long reconnectTime = minInterval;

            if (reconnectCount > 3){
                url = DEF_URL;
                long temp = minInterval * (reconnectCount -2);
                reconnectTime = temp > maxInterval ? maxInterval : temp;

            }

            Log.d(TAG, "reconnect: 准备开始底"+reconnectCount+"次重连,重连间隔"+reconnectTime+" --- url "+url);
        }

    }


    private Runnable mReconnectTask = new Runnable() {
        @Override
        public void run() {
            try {
                ws = new WebSocketFactory().createSocket(url,CONNECT_TIMEOUT)
                        .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                        .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                        .addListener(mListener = new WsListener())//添加回调监听
                        .connectAsynchronously();//异步连接

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void cancelReconnect(){
        reconnectCount = 0;
        mHandler.removeCallbacks(mReconnectTask);
    }

    private boolean isNetConnect(){

        ConnectivityManager connectivity = (ConnectivityManager) WsApplication.getInstance()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }
}
