package com.iflytek.webapi.ttspool;

import com.iflytek.webapi.utils.JsonUtil;
import com.iflytek.webapi.wspool.Response;

public class TTSResonse extends Response {

    private Data data;

    public Data getData() {
        return data;
    }

    public static TTSResonse decodeFromJson(String json){
        return JsonUtil.gson.fromJson(json, TTSResonse.class);
    }

    public static class Data{
        int status;
        String  audio;
        String ced;

        public int getStatus() {
            return status;
        }

        public String getAudio() {
            return audio;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "status=" + status +
                    ", audio=" + audio +
                    ", ced=" + ced +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "IgrResonse{" +
                "super="+super.toString()+
                "data=" + data +
                "data=" + data +
                '}';
    }
}
