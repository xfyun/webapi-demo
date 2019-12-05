package com.iflytek.webapi.ttspool;

import com.iflytek.webapi.wspool.IResponse;
import com.iflytek.webapi.wspool.WsClient;
import com.iflytek.webapi.wspool.WsConnectFailedException;

import java.io.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TTSWebsocketClient extends WsClient  {

    private String appid;
    private String uid;
    private String file;  //打开的数据文件
    private AtomicInteger sessionStatus = new AtomicInteger(0);
    public static final int StatusEnd  = 2;
    private FileOutputStream audioFileOutputStream; // 保存合成的音频
    public TTSWebsocketClient(String appid, String file) throws WsConnectFailedException, InterruptedException, IOException {
        super();
        this.appid = appid;
        this.file = file;

    }



    @Override
    public void onResult(IResponse response) {
        TTSResonse resp = (TTSResonse) response;  //响应结果需要实现IResponse 接口
        System.out.println("onResult=>"+resp);  //
        if (resp.getCode()==0){
            if (resp.getData()!=null){
                byte[] audio =  Base64.getDecoder().decode(resp.getData().audio);
                try {
                    audioFileOutputStream.write(audio);
                    audioFileOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                return;
            }
            sessionStatus.set(resp.getData().getStatus());
            if (sessionStatus.get()==2){ // session status ==2 标识所有结果已经返回，客户端可以关闭了
                close();

            }

        }else{
            close();
        }
    }


    @Override
    public void run() {
        try{

            File o = new File("tts_output"+super.getCid().toString()+".pcm");
            if (!o.exists()){
                o.createNewFile();
            }
            audioFileOutputStream = new FileOutputStream(o);

            File f = new File(file);
            if (!f.exists()) {
                throw new FileNotFoundException(file + ":file does not exists");
            }
            int size = 1280;
            byte[] buf = new byte[size];
            FileInputStream is = new FileInputStream(f);
            byte[] data = is.readAllBytes();
            is.close();
           sendData(data);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void sendRequest(TTSRequest request){
        if (sessionStatus.intValue() == StatusEnd){
            return;
        }
        super.send(request.toString());
    }

    public void sendData(byte[] text){
        TTSRequest request = new TTSRequest();
        request.setCommon(new TTSRequest.Common()
                .setApp_id(appid)
                .setUid(uid)
                .setCid(getCid().toString())
        );
        Map<String,Object> business = new HashMap<>();
        business.put("tte","utf8");
        business.put("vcn","xiaoyan");
        business.put("aue","raw");
        request.setBusiness(business);
        request.setData(new TTSRequest.Data()
                .setText(Base64.getEncoder().encodeToString(text))
                .setStatus(2)
        );
     //   System.out.println("req=>"+request);
//        System.out.println("req+requset"+request.toString());
        sendRequest(request);
    }


    //发送close帧关闭当前会话
    public void sendClose(){
        TTSRequest request = new TTSRequest();
        request.setCommon(new TTSRequest.Common()
                .setApp_id(appid)
                .setUid(uid)
                .setCid(getCid().toString())
                .setCmd("close")  // cmd=close 关闭当前frame
        );
        request.setData(new TTSRequest.Data().setStatus(2)
        );
        System.out.println("reques close=>"+request.toString());
        send(request.toString());
    }

    public void close(){
        try {
            audioFileOutputStream.flush();
            audioFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sendClose();
        System.out.println("close session");
        sessionStatus.set(2);
        super.close();
    }

}
