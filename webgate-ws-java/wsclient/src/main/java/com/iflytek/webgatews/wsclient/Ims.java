package com.iflytek.webapi.aiaas;

import com.google.gson.Gson;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;


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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class Ims extends WebSocketListener {
    private static final String hostUrl = "wss://ims.xfyun.cn/ims/v2/ims"; //http url 不支持解析 ws/wss schema
    private static final String apiKey = "xxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxx";
    private static final String appid = "xxxxxxxxx";
    private static final String file = "/Users/sjliu/go/src/git.xfyun.cn/AIaaS/webgate-ws/aclient_example/audio/ims.jpg";
    public static final int StatusFirstFrame = 0;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();


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
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        System.out.println(t.getMessage());
        try {
            System.out.println(response);
            if (response == null) {
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
        String authUrl = assembleRequestUrl(hostUrl, apiKey, apiSecret, "GET");

        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        Request request = new Request.Builder().url(authUrl).build();
        // System.out.println(client.newCall(request).execute());

        System.out.println("url===>" + authUrl);

        try {
            WebSocket webSocket = client.newWebSocket(request, new Ims());

            try (FileInputStream fs = new FileInputStream(file)) {

                JsonObject frame = new JsonObject();
                JsonObject business = new JsonObject();  //第一帧必须发送
                JsonObject common = new JsonObject();  //第一帧必须发送
                JsonObject data = new JsonObject();  //每一帧都要发送
                // 填充common
                common.addProperty("app_id", appid);
                //填充business
                //  business.addProperty("dwa", "wpgs");
                business.addProperty("ent", "image-structure");
                business.addProperty("func", "image/object");

                //填充data
                data.addProperty("status", 2);
                data.addProperty("image", Base64.getEncoder().encodeToString(fs.readAllBytes()));
                //填充frame
                frame.add("common", common);
                frame.add("business", business);
                frame.add("data", data);

                webSocket.send(frame.toString());
                System.out.println("send first");

                System.out.println("all data is send");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("end===");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 1; i++) {

            main1(null);
            System.out.println("fin==>:" + i);
        }
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

    }


    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret, String method) {
        URL url = null;
        // 替换调schema前缀 ，原因是URL库不支持解析包含ws,wss schema的url
        String httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://", "https://");
        try {
            url = new URL(httpRequestUrl);
            //获取当前日期并格式化
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String host = url.getHost();
            if (url.getPort() != -1) {
                host = host + ":" + String.valueOf(url.getPort());
            }
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append(method).append(" ").
                    append(url.getPath()).append(" HTTP/1.1");
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);

            String authorization = String.format("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));

        } catch (Exception e) {
            throw new RuntimeException("assemble requestUrl error:" + e.getMessage());
        }
    }

    private SimpleDateFormat format = new SimpleDateFormat();

    public String getDate() {
        return format.format(new Date());
    }
}
