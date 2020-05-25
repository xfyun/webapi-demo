package com.iflytek.webapi.wspool;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WsConnection extends WebSocketClient {

    private String url;

    public WsConnection(URI serverUri) {
        super(serverUri);
    }

    public WsConnection(String url,URIBuilder builder){
        super(builder.newURI(url));
        this.url =url;
    }
    public WsConnection(URI serverUri, Draft protocolDraft) {
        super(serverUri, protocolDraft);
    }

    private ConcurrentHashMap<String, WsClient> callback  = new ConcurrentHashMap<String, WsClient>();  //call back 注册
    //注册回调函数
    public void  registerCallback(WsClient client){
        callback.put(client.getCid().toString(),client);
    }
    // 回话结束后一定要取消注册，否则会发生内存泄漏
    public void  unregisterCallback(WsClient client){
        callback.remove(client.getCid().toString());
    }

    private URIBuilder uriBuilder;
    @Override
    public void onMessage(String s) {
        IResponse response = decode(s);
        String wscid = response.getCid();
        if (wscid == null){   //客户端传了wscid 的情况，服务端正常是会带上wscid回来的
            onException(new Exception(s));
        }else{
            WsClient client = callback.get(response.getCid());
            if (client!=null){
                client.onResult(response);
            }
        }

    }

    @Override
    public boolean reconnectBlocking() throws InterruptedException {
        if (this.uriBuilder!= null){
            this.uri = this.uriBuilder.newURI(this.url);
        }
        return super.reconnectBlocking();
    }

    public int getSesionNum() {
        return callback.size();
    }

    protected void onException(Exception e){

    }

    @Override
    public void onClose(int i, String s, boolean b) {
        closeConnection();
    }

    @Override
    public void onError(Exception e) {
        closeConnection();
        onException(e);
    }



    @Override
    public void close() {
        super.close();
        closeConnection();
    }

    private void closeConnection(){
        for (Map.Entry<String ,WsClient>e :callback.entrySet()){
            callback.remove(e.getKey());
        }
    }

    public abstract IResponse decode(String s);// 解码，对返回的json进行解码，需要继承实现

}
