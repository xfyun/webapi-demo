package com.iflytek.webapi.iatpool;

import com.iflytek.webapi.utils.JsonUtil;

import java.util.Map;

public class IatRequest {

    Common common;
    Map<String,Object> business;
    Data data;

    public static class Common{
        String cid;
        String uid;
        String app_id;
        String cmd;

        public String getCmd() {
            return cmd;
        }

        public Common setCmd(String cmd) {
            this.cmd = cmd;
            return this;
        }

        public String getCid() {
            return cid;
        }

        public Common setCid(String cid) {
            this.cid = cid;
            return this;
        }

        public String getUid() {
            return uid;
        }

        public Common setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public String getApp_id() {
            return app_id;
        }

        public Common setApp_id(String app_id) {
            this.app_id = app_id;
            return this;
        }
    }

    public static class Data{
        String audio;
        int status;
        String format;
        String encoding;

        public String getAudio() {
            return audio;
        }

        public Data setAudio(String audio) {
            this.audio = audio;
            return this;
        }

        public int getStatus() {
            return status;
        }

        public Data setStatus(int status) {
            this.status = status;
            return this;
        }

        public String getFormat() {
            return format;
        }

        public Data setFormat(String format) {
            this.format = format;
            return this;
        }

        public String getEncoding() {
            return encoding;
        }

        public Data setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }
    }

    @Override
    public String toString() {
        return JsonUtil.toJson(this);
    }

    public Common getCommon() {
        return common;
    }

    public IatRequest setCommon(Common common) {
        this.common = common;
        return this;
    }

    public Map<String, Object> getBusiness() {
        return business;
    }

    public IatRequest setBusiness(Map<String, Object> business) {
        this.business = business;
        return this;
    }

    public Data getData() {
        return data;
    }

    public IatRequest setData(Data data) {
        this.data = data;
        return this;
    }
}
