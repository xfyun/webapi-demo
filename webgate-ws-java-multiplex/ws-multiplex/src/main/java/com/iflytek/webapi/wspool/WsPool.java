package com.iflytek.webapi.wspool;

import org.java_websocket.enums.ReadyState;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

public class WsPool{
    private int size;  //连接池的大小

    private List<WsConnection> pools;  // 连接池

    private AtomicLong loopseeds = new AtomicLong(0); //控制轮询的计数器

    private String requestUrl;   // 请求地址

    private WsConnectionFactory factory ;


    public WsPool(String requestUrl, WsConnectionFactory factory, int size) {
        this.requestUrl = requestUrl;
        this.factory = factory;
        this.size =size;
        if(size <=0){
            this.size = 5;
        }
    }
    public WsPool(WsConnectionFactory factory,int size){
        this("",factory,size);
    }

    /**
     * 初始化连接池
     */
    public void init() throws WsConnectFailedException, URISyntaxException, InterruptedException {
        pools = new ArrayList<WsConnection>(size);
        connect();
    }
    //初始化连接
    private void connect() throws WsConnectFailedException, URISyntaxException, InterruptedException {
        URI uri = new URI(requestUrl);
        Semaphore semaphore = new Semaphore(-this.size+1);
        for (int i=0;i<size ;i++){
            new Thread(()->{
                WsConnection conn = factory.createConnection(uri);
                boolean ok = false;
                try {
                    ok = conn.connectBlocking();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(ok){
                    synchronized (this){
                        pools.add(conn);
                    }
                }
                semaphore.release();
            }).start();
          //  client.run();
        }
        semaphore.acquire();
        if(pools.size()<this.size){
            shutdown();
            throw new WsConnectFailedException();
        }
    }

    //获取一个可用的连接，多个会话可以并发复用一个连接，
    public WsConnection getConnection() throws InterruptedException, WsConnectFailedException {
        WsConnection client = pools.get((int)loopseeds.addAndGet(1)%size);

        if (client.getReadyState() != ReadyState.OPEN){ //连接已经关闭，重新连接
            synchronized (client){
                if (client.getReadyState() == ReadyState.OPEN){
                    return client;
                }
                boolean ok = client.reconnectBlocking();
                if (!ok){
                    throw new WsConnectFailedException("websocket reconnect failed");
                }
            }

        }

        return client;
    }
    //拿到一个连接并且执行
    public void execute(WsClient client) throws Exception {
        WsConnection connection = getConnection();
        client.initByConnection(connection);
        client.run();
    }

    public void shutdown(){
        for (WsConnection conn : pools) {
            conn.close();
        }
    }

}
