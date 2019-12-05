package com.iflytek.webgatews.wsclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import okio.ByteString;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class IatDemo extends WebSocketListener {
    private static final String hostUrl = "ws://iat-api.xfyun.cn/v2/iat"; //http url 不支持解析 ws/wss schema
    private static final String apiKey = "xxxxxxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String appid = "565fb083";
    private static final String file = "0.pcm";
    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();
    Decoder decoder = new Decoder();


    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        System.out.println("open connection");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        ResponseData resp = json.fromJson(text, ResponseData.class);
        System.out.println(text);
        if (resp != null) {
            if (resp.getCode() != 0) {
                System.out.println("error=>" + resp.getMessage() + " sid=" + resp.getSid());
                return;
            }
            if (resp.getData() != null) {
                if (resp.getData().getResult() != null) {
                    Text te = resp.getData().getResult().getText();
                    // System.out.println(resp.getData().getResult().getText().text);
                    try {
                        decoder.decode(te);
                        System.out.println(decoder.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
                    System.out.println("result is ==》" + decoder.toString());
                    decoder.discard();
                    webSocket.close(1000,"");
                    System.exit(0);
                } else {
                    // todo 根据返回的数据处理
                }
            }
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        System.out.println(t.getMessage());
        try {
            System.out.println(response);
            if (response == null){
                return;
            }
            System.out.println(response.code());
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main1(String[] args) throws Exception {
        // 构建鉴权url
        String authUrl = AuthUtils.assembleRequestUrl(hostUrl, apiKey, apiSecret);

        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        Request request = new Request.Builder().url(authUrl).build();
        // System.out.println(client.newCall(request).execute());

        System.out.println("url===>" + authUrl);

        try {
            WebSocket webSocket = client.newWebSocket(request, new IatDemo());

            int frameSize = 1280; //每一帧音频的大小
            int intervel = 40;
            int status = 0;  // 音频的状态

            //FileInputStream fs = new FileInputStream("0.pcm");
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
                            JsonObject frame = new JsonObject();
                            JsonObject business = new JsonObject();  //第一帧必须发送
                            JsonObject common = new JsonObject();  //第一帧必须发送
                            JsonObject data = new JsonObject();  //每一帧都要发送
                            // 填充common
                            common.addProperty("app_id", appid);
                            //填充business
                            business.addProperty("aue", "raw");
                            business.addProperty("ent", "xfime");
                            business.addProperty("vinfo", 1);
                            business.addProperty("svad", 1);
                            business.addProperty("accent", "mandarin");
                            //  business.addProperty("dwa", "wpgs");
                            //填充data
                            data.addProperty("status", StatusFirstFrame);
                            data.addProperty("format", "audio/L16;rate=16000");
                            data.addProperty("encoding", "raw");
                            data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            //填充frame
                            frame.add("common", common);
                            frame.add("business", business);
                            frame.add("data", data);

                            webSocket.send(frame.toString());
                            status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                            System.out.println("send first");
                            break;

                        case StatusContinueFrame:  //中间帧status = 1
                            JsonObject frame1 = new JsonObject();
                            JsonObject data1 = new JsonObject();
                            data1.addProperty("status", StatusContinueFrame);
                            data1.addProperty("format", "audio/L16;rate=16000");
                            data1.addProperty("encoding", "raw");
                            data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            frame1.add("data", data1);
                            webSocket.send(frame1.toString());
                            //   System.out.println("send continue");
                            break;

                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                            JsonObject frame2 = new JsonObject();
                            JsonObject data2 = new JsonObject();
                            data2.addProperty("status", StatusLastFrame);
                            data2.addProperty("audio", "");
                            data2.addProperty("format", "audio/L16;rate=16000");
                            data2.addProperty("encoding", "raw");
                            frame2.add("data", data2);
                            webSocket.send(frame2.toString());
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
            System.out.println("end===");
        }catch (Exception e){
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws Exception {
        for (int i=0 ;i<1 ;i++){

            main1(null);
            System.out.println("fin==>:"+i);
        }
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


    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSid() {
            return sid;
        }

        public Data getData() {
            return data;
        }
    }

    public static class Data {
        private int status;
        private Result result;

        public int getStatus() {
            return status;
        }

        public Result getResult() {
            return result;
        }
    }

    public static class Result {
        int bg;
        int ed;
        String pgs;
        int[] rg;
        int sn;
        Ws[] ws;

        public Text getText() {
            Text text = new Text();
            StringBuilder sb = new StringBuilder();
            for (Ws ws : this.ws) {
                sb.append(ws.toString());
            }
            text.sn = this.sn;
            text.text = sb.toString();
            text.sn = this.sn;
            text.rg = this.rg;
            text.dwa = this.pgs;
            return text;
        }
    }

    public static class Ws {
        Cw[] cw;
        int bg;

        public String toString(){
            StringBuilder sb = new StringBuilder();
            for (Cw cw:this.cw){
                sb.append(cw.w);
            }
            return sb.toString();
        }
    }

    public static class Cw {
        int sc;
        String w;

    }

    public static class Text {
        int sn;
        String text;
        String dwa;
        int[] rg;
        boolean deleted;

        @Override
        public String toString() {
            return "Text{" +
                    "sn=" + sn +
                    ", text='" + text + '\'' +
                    ", dwa='" + dwa + '\'' +
                    ", rg=" + Arrays.toString(rg) +
                    ", deleted=" + deleted +
                    '}';
        }
    }

    //解析返回数据，仅供参考
    public static class Decoder {
        private Text[] texts;
        private int defc = 10;

        public Decoder() {
            this.texts = new Text[this.defc];
        }

        public synchronized void decode(Text text) {
            if (text.sn >= this.defc) {
                this.resize();
                this.decode(text);
                return;
            }

            if ("rpl".equals(text.dwa)) {
                for (int i = text.rg[0]; i <= text.rg[1]; i++) {
                    this.texts[i].deleted = true;
                }
            }
            this.texts[text.sn] = text;

        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Text t : this.texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text);
                }
            }
            return sb.toString();
        }

        public void resize() {
            int oc = this.defc;
            this.defc <<= 1;
            Text[] old = this.texts;
            this.texts = new Text[this.defc];
            for (int i = 0; i < oc; i++) {
                this.texts[i] = old[i];
            }
        }

        public void discard(){
            for(int i=0;i<this.texts.length;i++){
                this.texts[i]= null;
            }
        }
    }
}
