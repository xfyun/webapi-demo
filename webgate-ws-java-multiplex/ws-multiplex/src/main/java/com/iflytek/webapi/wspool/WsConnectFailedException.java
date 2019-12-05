package com.iflytek.webapi.wspool;

public class WsConnectFailedException extends Exception {
    public WsConnectFailedException() {
        super("failed to connect to server");
    }

    public WsConnectFailedException(String message) {
        super(message);
    }
}
