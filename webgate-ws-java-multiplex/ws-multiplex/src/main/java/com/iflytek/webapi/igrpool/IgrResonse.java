package com.iflytek.webapi.igrpool;

import com.iflytek.webapi.utils.JsonUtil;
import com.iflytek.webapi.wspool.Response;

public class IgrResonse extends Response {

    private Data data;

    public Data getData() {
        return data;
    }

    public static IgrResonse decodeFromJson(String json){
        return JsonUtil.gson.fromJson(json, IgrResonse.class);
    }

    public static class Data{
        int status;
        Object result;

        public int getStatus() {
            return status;
        }

        public Object getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "status=" + status +
                    ", result=" + result +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "IgrResonse{" +
                "super="+super.toString()+
                "data=" + data +
                '}';
    }
}
