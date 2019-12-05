package com.iflytek.webapi.wspool;

public abstract class WsClient  implements Runnable{

    private WsConnection conn;

    private Wscid cid;   //用于标识该连接上的回话。
    public WsClient() throws WsConnectFailedException, InterruptedException {

    }
    //用于pool调用时设置一个可用的连接

    public void initByConnection(WsConnection conn){
      //  conn = pool.getConnection();
        this.conn = conn;
        this.cid = WsCidGenerator.generateWscid();
        conn.registerCallback(this);
    }

    public Wscid getCid() {
        return cid;
    }


    public void send(String text){
        this.conn.send(text);
    }

    /**
     *
     */
    public void close(){
        //conn.send();
        conn.unregisterCallback(this);
    }

    /**
     *
     *  pool 执行调用，请勿自己调用
     */
    public abstract void run();

    /**
     * 响应结果回调函数
     * @param response
     */
    public abstract void  onResult(IResponse response);
}
