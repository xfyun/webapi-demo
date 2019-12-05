package com.iflytek.webapi.wspool;

/**
 * 用于标识一个连接上的唯一回话，建议自增，会从响应结果带回给客户端
 */
public class Wscid {
    public Wscid(long id) {
        this.id = id;
    }

    private long id;
    @Override
    public String toString() {
        return Long.toString(id);
    }
}
