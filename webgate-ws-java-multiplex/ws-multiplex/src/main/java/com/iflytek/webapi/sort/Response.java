package com.iflytek.webapi.sort;

public class Response {
    private int sn;
    private int[] rg;
    private Object[] ws;

    public int getSn() {
        return sn;
    }

    public Response setSn(int sn) {
        this.sn = sn;
        return this;
    }

    public int[] getRg() {
        return rg;
    }

    public void setRg(int[] rg) {
        this.rg = rg;
    }

    public Object[] getWs() {
        return ws;
    }

    public void setWs(Object[] ws) {
        this.ws = ws;
    }
}
