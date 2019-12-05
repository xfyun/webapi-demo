package com.iflytek.webapi.igrpool;
import com.iflytek.webapi.utils.AuthUtils;
import com.iflytek.webapi.wspool.WsPool;

public class Test {

    public static final String serverUrl = "ws://ws-api.xfyun.cn/v2/igr";

    public static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxx";
    public static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxx";
    public static final String appid = "xxxxxxx";

    public static final String  file = "0.pcm";
    public static void main(String[] args) throws InterruptedException {

        String url = AuthUtils.assembleRequestUrl(serverUrl, apiKey, apiSecret);
        url = url+"&stream_mode=multiplex";//开启长连接多路复用模式
        WsPool pool = new WsPool(url, new IgrConnectonFactory(), 2);
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
                IgrWebsocketClient client = new IgrWebsocketClient(appid, file);
                pool.execute(client);
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
