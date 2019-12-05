package com.iflytek.webapi.igrpool;

import com.iflytek.webapi.wspool.WsConnection;
import com.iflytek.webapi.wspool.WsConnectionFactory;

import java.net.URI;

public class IgrConnectonFactory extends WsConnectionFactory {

    public WsConnection createConnection(URI uri) {
        return new IgrWsConnection(uri);
    }
    
}
