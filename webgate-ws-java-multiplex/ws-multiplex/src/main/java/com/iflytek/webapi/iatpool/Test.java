package com.iflytek.webapi.iatpool;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

public class Test {

    public static final String serverUrl = "ws://iat-api.xfyun.cn/v2/iat";

    public static final String apiKey = "xxxxxxxxxxxxxxxx";
    public static final String apiSecret = "xxxxxxxxxxxxxxxxxx";
    public static final String appid = "xxxxxxx";
    public static void main(String[] args) throws InterruptedException {

        String url = AuthUtils.assembleRequestUrl(serverUrl, apiKey, apiSecret);
        url = url+"&stream_mode=multiplex";//开启长连接多路复用模式
        WsPool pool = new WsPool(url, new IatConnectonFactory(), 5);
        try {
            pool.init();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                start(pool);
            }).start();
        }
    }

    public static void start(WsPool pool) {
        for (; ; ) {
            try {
                IatWebsocketClient client = new IatWebsocketClient(appid, "0.pcm");
                pool.execute(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
