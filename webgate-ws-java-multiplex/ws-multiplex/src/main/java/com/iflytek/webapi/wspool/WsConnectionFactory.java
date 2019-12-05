package com.iflytek.webapi.wspool;

import java.net.URI;

public  abstract class WsConnectionFactory {
    public abstract WsConnection createConnection(URI uri);
}
