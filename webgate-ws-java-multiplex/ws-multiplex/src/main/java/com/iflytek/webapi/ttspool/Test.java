package com.iflytek.webapi.ttspool;
import com.iflytek.webapi.iatpool.IatConnectonFactory;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

import java.net.URI;
import java.net.URISyntaxException;

public class Test {

    public static final String serverUrl = "ws://tts-api.xfyun.cn/v2/tts";
//    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
//    public static final String appid = "xxxxxxx";
    public static final String appid = "xxxxxxx";

    public static final String  file = "tts.txt";   // 合成文本
    public static void main(String[] args){

        WsPool pool = new WsPool(new TTSConnectonFactory(serverUrl,o->{
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

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                start(pool);
            }).start();
        }

    }

    public static void start(WsPool pool) {
        for (; ; ) {
            try {
                TTSWebsocketClient client = new TTSWebsocketClient(appid, file);
                pool.execute(client);
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
