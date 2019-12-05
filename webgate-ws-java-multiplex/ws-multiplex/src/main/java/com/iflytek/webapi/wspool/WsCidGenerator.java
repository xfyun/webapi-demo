package com.iflytek.webapi.wspool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WsCidGenerator {

    private WsCidGenerator(){

    }

    private AtomicLong id = new AtomicLong(0);

    public Wscid getWsCid(){
        long wscid = id.addAndGet(1);
        return new Wscid(wscid);
    }

    private static WsCidGenerator instance = new WsCidGenerator();

    public static Wscid generateWscid(){
        return instance.getWsCid();
    }
}
