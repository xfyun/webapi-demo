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
//busydog
public class IstDemo extends WebSocketListener {
    private static final String hostUrl = "ws://ist-api.xfyun.cn/v2/ist"; //http url 不支持解析 ws/wss schema
    private static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String appid = "xxxxxxxx";
    private static final String file = "0.pcm";

    public static final int StatusFirstFrame = 0;
    public static final int StatusContinueFrame = 1;
    public static final int StatusLastFrame = 2;
    public static final Gson json = new Gson();


    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        System.out.println("open connection");
       // new Thread(()->{
            //连接成功，开始发送数据

       // }).start();
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
                  //  System.out.println(resp.getData().getResult().getText().text);

                }
                if (resp.getData().getStatus() == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    System.out.println("session end ");
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
        Request request = new Request.Builder().url(authUrl).build();
        // System.out.println(client.newCall(request).execute());

        System.out.println("url===>" + authUrl);

        try {
            WebSocket webSocket = client.newWebSocket(request, new IstDemo());

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
                            business.addProperty("language", "zh_cn");
                            business.addProperty("accent", "mandarin");
                            business.addProperty("domain", "ist");

                            business.addProperty("dwa", "wpgs");
                            business.addProperty("eos", 3000000);
                            business.addProperty("rate", "16000");
                            data.addProperty("status", StatusFirstFrame);

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
                            JsonObject contineuFrame = new JsonObject();
                            JsonObject data1 = new JsonObject();
                            data1.addProperty("status", StatusContinueFrame);
                            data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len)));
                            contineuFrame.add("data", data1);
                            webSocket.send(contineuFrame.toString());
                            //   System.out.println("send continue");
                            break;

                        case StatusLastFrame:    // 最后一帧音频status = 2 ，标志音频发送结束
                            JsonObject lastFrame = new JsonObject();
                            JsonObject data2 = new JsonObject();
                            data2.addProperty("status", StatusLastFrame);
                            data2.addProperty("encoding", "raw");
                            lastFrame.add("data", data2);
                            webSocket.send(lastFrame.toString());
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
