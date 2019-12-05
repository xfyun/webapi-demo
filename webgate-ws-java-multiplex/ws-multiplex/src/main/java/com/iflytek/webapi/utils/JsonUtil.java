package com.iflytek.webapi.utils;

import com.google.gson.Gson;

/**
 *
 */
public class JsonUtil {
    public static Gson gson = new Gson();
    public static String toJson(Object o){
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json,Class<T> tClass){
        return gson.fromJson(json,tClass);
    }

}
