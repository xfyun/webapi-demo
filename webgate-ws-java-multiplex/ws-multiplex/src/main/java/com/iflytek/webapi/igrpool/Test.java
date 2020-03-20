package com.iflytek.webapi.igrpool;
import com.iflytek.webapi.iatpool.IatConnectonFactory;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

import java.net.URI;
import java.net.URISyntaxException;

public class Test {

    public static final String serverUrl = "wss://ws-api.xfyun.cn/v2/igr";

//    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String appid = "xxxxxxx";
    public static final String appid = "xxxxxxx";

    public static final String  file = "0.pcm";
    public static void main(String[] args) throws InterruptedException {

        WsPool pool = new WsPool(new IgrConnectonFactory(serverUrl,o->{
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
        for (int i = 0; i < 1; i++) {
            new Thread(() -> {
                start(pool);
            }).start();
        }

    }

    public static void start(WsPool pool) {
        for (; ; ) {
            try {
                IgrWebsocketClient client = new IgrWebsocketClient(appid, file);
                pool.execute(client);
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
