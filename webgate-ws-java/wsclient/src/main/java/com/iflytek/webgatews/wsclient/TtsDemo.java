package com.iflytek.webgatews.wsclient;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import okio.ByteString;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

public class TtsDemo {
    private static final String hostUrl = "https://tts-api.xfyun.cn/v2/tts"; //http url 不支持解析 ws/wss schema
    private static final String apiKey = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String apiSecret = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    private static final String appid = "xxxxxxxx";
    private static final String text = "哈撒给";
    public static final Gson json = new Gson();

    public static void main(String[] args) throws Exception {
        // 构建鉴权url
        String authUrl = getAuthUrl(hostUrl, apiKey, apiSecret);

        OkHttpClient client = new OkHttpClient.Builder().build();
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        String url = authUrl.toString().replace("http://", "ws://").replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        // 存放音频的文件
        File f = new File("audio.pcm");
        if (!f.exists()) {
            f.createNewFile();
        }

        FileOutputStream os = new FileOutputStream(f);
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                try {
                    System.out.println(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //发送数据
                JsonObject frame = new JsonObject();
                JsonObject business = new JsonObject();
                JsonObject common = new JsonObject();
                JsonObject data = new JsonObject();
                // 填充common
                common.addProperty("app_id", appid);
                //填充business
                business.addProperty("aue", "lame");
                business.addProperty("vcn", "qianhui");
                business.addProperty("ent", "mtts");
                business.addProperty("tte", "unicode");
                business.addProperty("volume", 50);
                business.addProperty("pitch", 50);
                business.addProperty("speed", 50);
                //填充data
                data.addProperty("status", 2);
                try {
                    data.addProperty("text", Base64.getEncoder().encodeToString(text.getBytes("utf8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //填充frame
                frame.add("common", common);
                frame.add("business", business);
                frame.add("data", data);
                webSocket.send(frame.toString());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                //处理返回数据
             //   System.out.println("receive=>" + text);
                ResponseData resp = null;
                try {
                    resp = json.fromJson(text, ResponseData.class);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("resp=>:"+resp.toString());

                if (resp != null) {
                    if (resp.getCode() != 0) {
                        System.out.println("error=>" + resp.getMessage() + " sid=" + resp.getSid());
                        return;
                    }
                    if (resp.getData() != null) {

                        String result = resp.getData().audio;
                        byte[] audio = Base64.getDecoder().decode(result);
                        try {

                            String sp = resp.getData().spell;
                            if (sp !=null){

                                   byte [] spell = Base64.getDecoder().decode(resp.getData().spell);
                                System.out.println("spell:"+new String(spell,"gb2312"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        try {
                            os.write(audio);
                            os.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (resp.getData().status == 2) {
                            // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                            System.out.println("session end ");
                            webSocket.close(1000, "");
                            System.exit(0);
                            try {
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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
                System.out.println("connection failed");
            }
        });


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
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
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

        @Override
        public String toString() {
            return "ResponseData{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    ", sid='" + sid + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    public static class Data {
        private int status;  //标志音频是否返回结束  status=1，表示后续还有音频返回，status=2表示所有的音频已经返回
        private String audio;  //返回的音频，base64 编码
        private String ced;  // 合成进度
        private String spell;  //音频的拼音标注

        @Override
        public String toString() {
            return "Data{" +
                    "status=" + status +
                    ", ced='" + ced + '\'' +
                    ", spell='" + spell + '\'' +
                    '}';
        }
    }


}
