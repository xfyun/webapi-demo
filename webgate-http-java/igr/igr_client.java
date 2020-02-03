package com.authentic.http;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class igr_client {
    private static final String HOST_URL = "https://rest-api-dx.xfyun.cn/v2/igr";
    private static final String API_KEY = "48xxxxxxxxxxxxxxx";
    private static final String API_SECRET = "Tx74pxxxxxxxxxxxx";
    private static final String body = "{\"business\": {\"aue\": \"raw\", \"rate\": \"16000\", \"ent\": \"igr\"}, \"common\": {\"app_id\": \"5xxxxxxx\"},\"data\": {\"audio\": \"";
    public static final MediaType JSON = MediaType.parse("application/json;charset=utf-8");
    private static final String path = "./webgate-http-java/com/authentic/http/igr/igrtest.pcm";

    public static void main(String[] args) throws Exception {
        Request req = getRequest();
        Response resp = doAuthtic(req);
        System.out.println(resp.code() + ":" + resp.body().string());
    }


    private static String add(String body) {
        String res = null;
        res = body + fileToBase64(path).toString() + "\"}}";
        return res.toString();
    }

    private static String fileToBase64(String path) {
        String base64 = null;
        InputStream in = null;
        try {
            File file = new File(path);
            in = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            in.read(bytes);
            base64 = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return base64;
    }

    public static Response doAuthtic(Request request) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder().build();
        return client.newCall(request).execute();
    }

    public static Request getRequest() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        String digest = "SHA-256=" + signBody();
        RequestBody requestBody = RequestBody.create(JSON, add(body));
        Request request = new Request.Builder().url(HOST_URL).//
                addHeader("Content-Type", "application/json").//
                addHeader("Date", date).//
                addHeader("Digest", digest).//
                addHeader("Authorization", getAuthorization(generateSignature(digest, date))).//
                post(requestBody).//
                build();
        return request;
    }

    private static String signBody() throws Exception {
        MessageDigest messageDigest;
        String encodestr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes("UTF-8"));
            encodestr = Base64.getEncoder().encodeToString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodestr;
    }

    private static String generateSignature(String digest, String date) throws Exception {
        URL url = new URL(HOST_URL);
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost());
        if (url.getPort() != -1) {
            builder.append(":").append(url.getPort());
        }
        builder.append("\n").//
                append("date: ").append(date).append("\n").//
                append("POST ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digest);
        return hmacsign(builder.toString(), API_SECRET);
    }

    private static String getAuthorization(String sign) {
        return String.format("hmac api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", //
                API_KEY, "hmac-sha256", "host date request-line digest", sign);
    }

    private static String hmacsign(String signature, String apiSecret) throws Exception {
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signature.getBytes(charset));
        return Base64.getEncoder().encodeToString(hexDigits);
    }

}
