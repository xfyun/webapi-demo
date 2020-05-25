package com.iflytek.webapi.iatpool;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

import java.net.URI;
import java.net.URISyntaxException;

public class Test {

    public static final String serverUrl = "wss://iat-api.xfyun.cn/v2/iat";

//    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String appid = "xxxxxxx";
    public static final String appid = "xxxxxxx";
    public static void main(String[] args) throws InterruptedException {

//        String url = ;
//        url = url+"&stream_mode=multiplex";//开启长连接多路复用模式
        WsPool pool = new WsPool(new IatConnectonFactory(serverUrl,o->{
            try {
                return new URI( AuthUtils.assembleRequestUrl(o, apiKey, apiSecret)+"&stream_mode=multiplex");
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            return null;
        }), 1);
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
