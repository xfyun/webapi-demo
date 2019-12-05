package com.iflytek.webgatews.wsclient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @Author:sjliu7
 * 鉴权使用
 * @Date:2019/7/31 15:23
 */
public class AuthUtils {

    /**
     * 生成用于鉴权的URL
     * @param requestUrl
     * @param apiKey
     * @param apiSecret
     * @return
     */
    /**
     * host: ws-api.xfyun.cn
     * date: Mon, 12 Aug 2019 01:46:29 GMT
     * GET /v2/iat HTTP/1.1
     * @param requestUrl
     * @param apiKey
     * @param apiSecret
     * @return
     */
    public static String assembleRequestUrl(String requestUrl, String apiKey, String apiSecret) {
        URL url = null;
        String  httpRequestUrl = requestUrl.replace("ws://", "http://").replace("wss://","https://" );
        try {
            url = new URL(httpRequestUrl);
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            String host = url.getHost();
            StringBuilder builder = new StringBuilder("host: ").append(host).append("\n").//
                    append("date: ").append(date).append("\n").//
                    append("GET ").append(url.getPath()).append(" HTTP/1.1");
            Charset charset = Charset.forName("UTF-8");
            Mac mac = Mac.getInstance("hmacsha256");
            System.out.println(builder.toString());
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
            String sha = Base64.getEncoder().encodeToString(hexDigits);
            String authorization = String.format("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
            String authBase = Base64.getEncoder().encodeToString(authorization.getBytes(charset));
            return String.format("%s?authorization=%s&host=%s&date=%s", requestUrl, URLEncoder.encode(authBase), URLEncoder.encode(host), URLEncoder.encode(date));

        } catch (Exception e) {
          // throw new RuntimeException("assemble requestUrl error:"+e.getMessage());
           throw new RuntimeException(e);
        }

    }
}
