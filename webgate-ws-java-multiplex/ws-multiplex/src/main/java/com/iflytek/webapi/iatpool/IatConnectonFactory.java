package com.iflytek.webapi.iatpool;

import com.iflytek.webapi.wspool.WsConnection;
import com.iflytek.webapi.wspool.WsConnectionFactory;

import java.net.URI;

public class IatConnectonFactory extends WsConnectionFactory {
    public WsConnection createConnection(URI uri) {
        return new IatWsConnection(uri);
    }
}
