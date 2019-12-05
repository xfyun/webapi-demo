package com.iflytek.webapi.ttspool;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

public class Test {

    public static final String serverUrl = "ws://tts-api.xfyun.cn/v2/tts";

    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxx";

    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    public static final String appid = "xxxxxx";

    public static final String  file = "tts.txt";   // 合成文本
	
    public static void main(String[] args){

        String url = AuthUtils.assembleRequestUrl(serverUrl, apiKey, apiSecret);
        url = url+"&stream_mode=multiplex";//开启长连接多路复用模式
        WsPool pool = new WsPool(url, new TTSConnectonFactory(), 1);
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
