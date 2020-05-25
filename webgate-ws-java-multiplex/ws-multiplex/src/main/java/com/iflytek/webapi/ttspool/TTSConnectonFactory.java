package com.iflytek.webapi.ttspool;

import com.iflytek.webapi.wspool.URIBuilder;
import com.iflytek.webapi.wspool.WsConnection;
import com.iflytek.webapi.wspool.WsConnectionFactory;

import java.net.URI;

public class TTSConnectonFactory extends WsConnectionFactory {

    public WsConnection createConnection(URI uri) {
        return new TTSWsConnection(this.url,this.builder);
    }
    private String url;
    private URIBuilder builder;
    public TTSConnectonFactory(String url, URIBuilder builder){
        this.url = url;
        this.builder = builder;
    }
}
