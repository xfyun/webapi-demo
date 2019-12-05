# -*- coding:utf-8 -*-
import websocket
import requests
import datetime
import hashlib
import base64
import hmac
import json
import os, sys
import re
import urllib.request
from urllib.parse import urlencode
import logging
import time
type = sys.getfilesystemencoding()

path_pwd = os.path.split(os.path.realpath(__file__))[0]
os.chdir(path_pwd)

try:
    import thread
except ImportError:
    import _thread as thread

logging.basicConfig()

STATUS_FIRST_FRAME = 0  #第一帧的标识
STATUS_CONTINUE_FRAME = 1  #中间帧标识
STATUS_LAST_FRAME = 2  #最后一帧的标识

global wsParam


class Ws_Param(object):
    # 初始化
    def __init__(self, host):
        self.Host = host
        self.HttpProto = "HTTP/1.1"
        self.HttpMethod = "GET"
        self.RequestUri = "/v2/iat"
        self.APPID = "565fb083"
        self.Algorithm = "hmac-sha256"
        self.UserName = "4c2f2a8f2fde911ed422291161c4c598"
        self.Secret = "Tx7m1B5xx1wyEtfBeeso3pdUX6yzMg4p"
        # 设置url
        if re.match("^\d", self.Host):
            self.url = "ws://" + self.Host + self.RequestUri
        else:
            self.url = "wss://" + self.Host + self.RequestUri

        # 设置当前时间
        curTime_utc = datetime.datetime.utcnow()
        self.Date = self.httpdate(curTime_utc)

        # 设置测试音频文件
        self.AudioFile = "./test10s.ogg"
        self.CommonArgs = {"app_id": self.APPID}
        self.BusinessArgs = {"dwa": "wpgs", "language": "zh_cn"}


    def httpdate(self, dt):
        """
        Return a string representation of a date according to RFC 1123
        (HTTP/1.1)
        The supplied date must be in UTC.
        """
        weekday = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][dt.weekday()]
        month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
                 "Oct", "Nov", "Dec"][dt.month - 1]
        return "%s, %02d %s %04d %02d:%02d:%02d UTC" % (weekday, dt.day, month, dt.year, dt.hour, dt.minute, dt.second)

    def generateSignature(self):
        signatureStr = "host: " + self.Host + "\n"
        signatureStr += "date: " + self.Date + "\n"
        signatureStr += self.HttpMethod + " " + self.RequestUri + " " + self.HttpProto
        signature = hmac.new(self.Secret.encode(encoding='utf-8'),
                             signatureStr.encode(encoding='utf-8'),
                             digestmod=hashlib.sha256).digest()
        result = base64.b64encode(signature)
        return result.decode(encoding='utf-8')

    def init_url(self):
        sign = self.generateSignature()
        authorization = 'hmac username="%s", algorithm="%s", ' \
                     'headers="host date request-line", ' \
                     'signature="%s"' \
                        % (self.UserName, self.Algorithm, sign)
        url_values = {"authorization": base64.b64encode(authorization.encode(encoding='utf-8')),
                      "host": self.Host, "date": self.Date}
        url = self.url+"?"+urlencode(url_values)
        return url


# 收到websocket消息的处理
def on_message(ws, message):
    try:
        # code = json.loads(message)["code"]
        # sid = json.loads(message)["sid"]
        # if code != 0:
        #     err_msg = json.loads(message)["message"]
        #     print("sid:%s call error:%s code is:%s" % (sid, err_msg, code))
        if message != "":
              res = json.loads(message.decode(encoding='utf-8'))
              print(res)
        else:
            print("### response is nil")
            #else:
             #    data = json.loads(message)["data"]
              #   print("sid:%s call success!,data is:%s" % (sid, json.dumps(data, ensure_ascii=False)))
    except Exception as e:
        print("receive msg,but parse exception:", e)

# 收到websocket错误的处理
def on_error(ws, error):
    print("### error:", error)

# 收到websocket关闭的处理
def  on_close(ws):
    print("### closed ###")

# 收到websocket连接建立的处理
def on_open(ws):
    def run(*args):
        frameSize = 1220 #每一帧的音频大小
        intervel = 0.04 #发送音频间隔(单位:s)
        status = STATUS_FIRST_FRAME #音频的状态信息，标识音频是第一帧，还是中间帧、最后一帧
        with open(wsParam.AudioFile, "rb") as fp:
            while True:
                buf = fp.read(frameSize)
                #文件结束
                if buf == '':
                    status = STATUS_LAST_FRAME
                # 第一帧处理
                # 发送第一帧音频，带business 参数
                # appid 必须带上，只需第一帧发送
                if status == STATUS_FIRST_FRAME:
                    d = {"common": wsParam.CommonArgs,
                         "business": wsParam.BusinessArgs,
                         "data": {"status": 0, "format": "audio/L16;rate=16000",
                                 "data_type": 1, "audio": base64.b64encode(buf), "encoding": "opus-ogg"}}
                    ws.send(json.dumps(d))
                    status = STATUS_CONTINUE_FRAME
                #中间帧处理
                elif status == STATUS_CONTINUE_FRAME:
                    d = {"data": {"status": 1, "format": "audio/L16;rate=16000",
                                  "data_type": 1, "audio": base64.b64encode(buf), "encoding": ""}}
                    ws.send(json.dumps(d))
                # 最后一帧处理
                elif status == STATUS_LAST_FRAME:
                    d = {"data": {"status": 2, "format": "audio/L16;rate=16000",
                                  "data_type": 1, "audio": base64.b64encode(buf), "encoding": ""}}
                    ws.send(json.dumps(d))
                    time.sleep(1)
                    break
                # 模拟音频采样间隔
                time.sleep(intervel)
        ws.close()
    thread.start_new_thread(run, ())

if __name__ == "__main__":
    ##示例:  host="ws-api.xfyun.cn"域名形式 或者host="ip:port"形式  172.21.161.28:9066
    # host = sys.argv[1]
    wsParam = Ws_Param("ws-api.xfyun.cn")
    websocket.enableTrace(False)
    wsUrl = wsParam.init_url()
    ws = websocket.WebSocketApp(wsUrl, on_message=on_message, on_error=on_error, on_close=on_close)
    ws.on_open = on_open
    ws.run_forever()
