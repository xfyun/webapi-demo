package com.iflytek.webgatews.wsclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class IseDemo extends WebSocketListener {
    //private static final String hostUrl = "https://ise-api-dx.xfyun.cn/v2/see"; //http url 不支持解析 ws/wss schema
    private static final String hostUrl = "http://rest-api-gz.xfyun.cn/v2/see"; //调试地址 schema
    private static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String appid = "xxxxxxxxx";
    private static final String file = "en_sentence.pcm";
    private static final String text = "When you don't know what you're doing, it's helpful to begin by learning about what you should not do. ";

    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;

    public static final Gson json = new Gson();

    private String aus = "1";

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        new Thread(() -> {
            //连接成功，开始发送数据
            int frameSize = 9000; //每一帧音频的大小
            int intervel = 40;
            int status = 0;  // 音频的状态

            //FileInputStream fs = new FileInputStream("0.pcm");
            ssb(webSocket);
            //ttp(webSocket);
            try (FileInputStream fs = new FileInputStream(file)) {
                byte[] buffer = new byte[frameSize];
                // 发送音频
                end:
                while (true) {
                    int len = fs.read(buffer);
                    if (len == -1) {
                        status = StatusLastFrame;  //文件读完，改变status 为 2
                    }

                    switch (status) {
                        case StatusFirstFrame:   // 第一帧音频status = 0
                            send(webSocket,1,1,Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            break;

                        case StatusContinueFrame:  //中间帧status = 1
                            send(webSocket,2,1,Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            break;

                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                            send(webSocket,4,2,"");
                            System.out.println("sendlast");
                            break end;
                    }

                    Thread.sleep(intervel); //模拟音频采样延时
                }
                System.out.println("all data is send");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


    }

    private void ssb(WebSocket webSocket) {
        ParamBuilder p = new ParamBuilder();
        p.add("common", new ParamBuilder()
                .add("app_id", appid)
        )
                .add("business", new ParamBuilder()
                        .add("category", "read_sentence")
                        .add("ent", "en")
                      //  .add("res_id", "")
                        //.add("ssb_txt", "welcome to iflytek")
                      //  .add("tte", "utf-8")
                        .add("cmd", "ssb")
                        .add("ssm", "1")
                        .add("auf", "audio/L16;rate=16000")
                        .add("aue", "raw")
                        .add("text",text)
                ).add("data", new ParamBuilder()
                .add("status", 0)
                .add("data", "")
        );
        webSocket.send(p.toString());
    }

    private void ttp(WebSocket webSocket) {

        ParamBuilder p = new ParamBuilder();
        p.add("common", new ParamBuilder()
                .add("app_id", appid)
        )
                .add("business", new ParamBuilder()
                        .add("category", "read_sentence")
                        .add("sub", "see")
                        .add("subsvc", "en")
                        .add("ent", "en")
                        .add("res_id", "")
                        .add("ssb_txt", "welcome to iflytek")
                        .add("tte", "utf-8")
                        .add("cmd", "ttp")
                        .add("ssm", "1")
                        .add("cver", "5.0.5.1059")//客户端版本
                        .add("csid", "csi00c00002@uk16811b96a8a6b08880")//先固定
                        .add("key", "0d9kXCW9mXhqdzHCuHtVPBp^fIBXQ1Jt7ulThhr86MwfQ/l84ioAJBEUjVxNsscxD9")//先固定
                ).add("data", new ParamBuilder()
                .add("data", Base64.getEncoder().encodeToString("When you don't know what you're doing, it's helpful to begin by learning about what you should not do. ".getBytes()))//todo:此处传入待评测文本by base64编码
                .add("status", 1)

        );
        webSocket.send(p.toString());
    }

    public void send(WebSocket webSocket, int aus,int status, String data) {
        ParamBuilder p = new ParamBuilder();
        p.add("business", new ParamBuilder()
                .add("cmd", "auw")
                .add("aus", aus)
                .add("aue", "raw")
        ).add("data",new ParamBuilder()
                .add("status",status)
                .add("data",data)
                .add("data_type",1)
                .add("encoding","raw")
        );
       // System.out.println(p.toString());
        webSocket.send(p.toString());
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        System.out.println("response==>"+text);
    }

    public static void main(String[] args) throws Exception {
        // 构建鉴权url
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);
        OkHttpClient client = new OkHttpClient.Builder().build();
        System.out.println(authUrl);
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        String url = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();
        // System.out.println(client.newCall(request).execute());

        System.out.println("url===>" + url);

        WebSocket webSocket = client.newWebSocket(request, new IseDemo());


    }

    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());

        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = Base64.getEncoder().encodeToString(hexDigits);

        String authorization = String.format("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        HttpUrl httpUrl = HttpUrl.parse(hostUrl).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(charset))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }


    public static class ParamBuilder {
        private JsonObject jsonObject = new JsonObject();

        public ParamBuilder add(String key, String val) {
            this.jsonObject.addProperty(key, val);
            return this;
        }

        public ParamBuilder add(String key, int val) {
            this.jsonObject.addProperty(key, val);
            return this;
        }

        public ParamBuilder add(String key, boolean val) {
            this.jsonObject.addProperty(key, val);
            return this;
        }

        public ParamBuilder add(String key, float val) {
            this.jsonObject.addProperty(key, val);
            return this;
        }

        public ParamBuilder add(String key, JsonObject val) {
            this.jsonObject.add(key, val);
            return this;
        }

        public ParamBuilder add(String key, ParamBuilder val) {
            this.jsonObject.add(key, val.jsonObject);
            return this;
        }


        @Override
        public String toString() {
            return this.jsonObject.toString();
        }
    }

}

