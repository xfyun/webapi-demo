package com.iflytek.webgatews.wsclient;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import okhttp3.*;

import okio.ByteString;

public class IgrWsDemo {
    private static final String hostUrl = "ws://ws-api.xfyun.cn/v2/igr";
    private static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String appid = "xxxxxxxx";
    private static final String file = "0.pcm";

    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;

    public static final Gson json = new Gson();

    public static void main(String[] args) throws Exception {
        // 构建鉴权url
        String authUrl = AuthUtils.assembleRequestUrl(hostUrl, apiKey, apiSecret);

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder().url(authUrl).build();
       // System.out.println(client.newCall(request).execute());

        System.out.println("url===>" + authUrl);
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    System.out.println(response.body().string()+"open");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                System.out.println("receive=>" + text);
                ResponseData resp = json.fromJson(text, ResponseData.class);
                if (resp != null) {
                    if (resp.getCode() != 0) {
                        System.out.println("error=>" + resp.getMessage() + " sid=" + resp.getSid());
                        return;
                    }
                    if (resp.getData() != null) {
                        if (resp.getData().getStatus() == 2) {
                            // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                            System.out.println("session end ");
                            webSocket.close(1005,"");
                        } else {
                            // todo 根据返回的数据处理
                        }
                    }
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                System.out.println("socket closing");
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                System.out.println("socket closed");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                try {
                    System.out.println("connection failed");
                    if (response!=null){
                        System.out.println(response.body().string());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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
                        business.addProperty("rate", "16000");
                        business.addProperty("ent", "igr");

                        //填充data
                        data.addProperty("status", status);
                        data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer,len)));
                        //填充frame
                        frame.add("common", common);
                        frame.add("business", business);
                        frame.add("data", data);

                        webSocket.send(frame.toString());
                        System.out.println("send first"+frame.toString());
                        status = StatusContinueFrame;  // 发送完第一帧改变status 为 1
                        break;

                    case StatusContinueFrame:  //中间帧status = 1
                        JsonObject continueFrame = new JsonObject();
                        JsonObject data1 = new JsonObject();
                        data1.addProperty("status", status);
                        data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer,len)));
                        continueFrame.add("data", data1);
                        webSocket.send(continueFrame.toString());
                        break;

                    case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                        JsonObject lastFrame = new JsonObject();
                        JsonObject data2 = new JsonObject();
                        data2.addProperty("status", status);
                        data2.addProperty("audio", "");
                        lastFrame.add("data", data2);
                        webSocket.send(lastFrame.toString());
                        break end;
                }

                Thread.sleep(intervel); //模拟音频采样延时
            }
            System.out.println("all data is send");
        }catch (Exception e){
            e.printStackTrace();
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
        private Object result;

        public int getStatus() {
            return status;
        }

        public Object getResult() {
            return result;
        }
    }

}
