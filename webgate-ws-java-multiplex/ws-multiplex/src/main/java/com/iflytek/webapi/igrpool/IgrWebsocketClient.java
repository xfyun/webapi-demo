package com.iflytek.webapi.igrpool;

import com.iflytek.webapi.wspool.IResponse;
import com.iflytek.webapi.wspool.WsClient;
import com.iflytek.webapi.wspool.WsConnectFailedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class IgrWebsocketClient extends WsClient  {

    private String appid;
    private String uid;
    private String file;  //打开的音频文件
    private AtomicInteger sessionStatus = new AtomicInteger(0);
    public static final int StatusEnd  = 2;

    public IgrWebsocketClient(String appid, String file)throws WsConnectFailedException, InterruptedException {
        super();
        this.appid = appid;
        this.file = file;
    }

    @Override
    public void onResult(IResponse response) {
        IgrResonse iatresp = (IgrResonse) response;  //响应结果需要实现IResponse 接口
        System.out.println("onResult=>"+iatresp);  //
        if (iatresp.getCode()==0){
            sessionStatus.set(iatresp.getData().getStatus());
            if (sessionStatus.get()==2){ // session status ==2 标识所有结果已经返回，客户端可以关闭了
                close();
            }
        }else{
            close();
        }
    }


    @Override
    public void run() {
        try {
            int status = 0;
            File f = new File(file);
            if (!f.exists()) {
                throw new FileNotFoundException(file + ":file does not exists");
            }
            int size = 1280;
            byte[] buf = new byte[size];
            FileInputStream is = new FileInputStream(f);
            for (; ; ) {
                //已经拿到最后的结果不再发送
                if (sessionStatus.get() == 2) {
                    break;
                }
                int len = is.read(buf);
                if (len == -1) {
                    status = 2;
                }
                if (status == 0) {
                    sendFirst(buf);
                    status = 1;
                } else if (status == 1) {
                    send(Arrays.copyOf(buf, len), 1);
                } else {
                    send(Arrays.copyOf(buf, 0), 2);
                    break;
                }

                Thread.sleep(40);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void sendRequest(IgrRequest request){
        if (sessionStatus.intValue() == StatusEnd){
            return;
        }
        super.send(request.toString());
    }

    public void sendFirst(byte[] audio){
        IgrRequest request = new IgrRequest();
        request.setCommon(new IgrRequest.Common()
                .setApp_id(appid)
                .setUid(uid)
                .setCid(getCid().toString())
        );
        Map<String,Object> business = new HashMap<>();
        business.put("aue","raw");
        business.put("rate","16000");
        business.put("ent","igr");
        //  business.put("domain","iat");
        // business.put("accent","");
        request.setBusiness(business);
        request.setData(new IgrRequest.Data()
                .setAudio(Base64.getEncoder().encodeToString(audio))
                .setEncoding("raw")
                .setFormat("16000")
                .setStatus(0)
        );
     //   System.out.println("req=>"+request);
//        System.out.println("req+requset"+request.toString());
        sendRequest(request);
    }

    public void send(byte[] audio,int status){
        IgrRequest request = new IgrRequest();
        request.setCommon(new IgrRequest.Common()
                .setApp_id(appid)
                .setUid(uid)
                .setCid(getCid().toString())
        );
      //  business.put("domain","iat");
       // business.put("accent","");
        request.setData(new IgrRequest.Data()
                .setAudio(Base64.getEncoder().encodeToString(audio))
                .setStatus(status)
        );
        sendRequest(request);
    }

    //发送close帧关闭当前会话
    public void sendClose(){
        IgrRequest request = new IgrRequest();
        request.setCommon(new IgrRequest.Common()
                .setApp_id(appid)
                .setUid(uid)
                .setCid(getCid().toString())
                .setCmd("close")  // cmd=close 关闭当前frame
        );
        request.setData(new IgrRequest.Data().setStatus(2)
        );
        System.out.println("reques close=>"+request.toString());
        send(request.toString());
    }

    public void close(){
        sendClose();
        System.out.println("close session");
        sessionStatus.set(2);
        super.close();
    }

}
