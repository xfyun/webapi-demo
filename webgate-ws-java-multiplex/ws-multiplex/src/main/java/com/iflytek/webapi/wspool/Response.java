package com.iflytek.webapi.wspool;

public abstract class Response implements IResponse {
    private String sid;
    private String message;
    private int code;
    private String cid;
    public String getSid() {
        return sid;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    public String getCid() {
        return cid;
    }

    @Override
    public String toString() {
        return "Response{" +
                "sid='" + sid + '\'' +
                ", message='" + message + '\'' +
                ", code=" + code +
                ", cid='" + cid + '\'' +
                '}';
    }
}
