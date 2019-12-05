#!/usr/bin/python2 
# -*- coding:utf-8 -*-

import requests
import datetime
import hashlib
import base64
import hmac
import json
import os,sys
import re

path_pwd = os.path.split(os.path.realpath(__file__))[0]
os.chdir(path_pwd)


class get_result(object):
    def __init__(self,host):
        # 以下为POST请求
        self.Host = host
        self.RequestUri = "/v2/iat"
        #设置url
        if re.match("^\d", self.Host):
            #print(host)
            self.url="http://"+host+self.RequestUri
        else:
             #print(host)
             self.url="https://"+host+self.RequestUri
        self.HttpMethod = "POST"
        self.APPID = "565fb083"
        self.Algorithm = "hmac-sha256"
        self.HttpProto = "HTTP/1.1"
        self.UserName="4c2fxxxxxxxxxxxxxxxxx"
        self.Secret = "Txxxxxxxxxxxxxxxxxxx"

        # 设置当前时间
        curTime_utc = datetime.datetime.utcnow()
        self.Date = self.httpdate(curTime_utc)
        #设置测试音频文件
        self.AudioPath="./xiao.pcm"
        self.BusinessArgs={
                "language": "zh_cn",
                "domain":   "vanke",
                "accent":   "mandarin",
            }

    def imgRead(self, path):
        with open(path, 'rb') as fo:
            return fo.read()

    def hashlib_256(self, res):
        m = hashlib.sha256(bytes(res.encode(encoding='utf-8'))).digest()
        result = "SHA-256=" + base64.b64encode(m).decode(encoding='utf-8')
        return result

    def httpdate(self, dt):
        """
        Return a string representation of a date according to RFC 1123
        (HTTP/1.1).

        The supplied date must be in UTC.

        """
        weekday = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][dt.weekday()]
        month = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep",
                 "Oct", "Nov", "Dec"][dt.month - 1]
        return "%s, %02d %s %04d %02d:%02d:%02d GMT" % (weekday, dt.day, month,
                                                        dt.year, dt.hour, dt.minute, dt.second)

    def generateSignature(self, digest):
        signatureStr = "host: " + self.Host + "\n"
        signatureStr += "date: " + self.Date + "\n"
        signatureStr += self.HttpMethod + " " + self.RequestUri \
                        + " " + self.HttpProto + "\n"
        signatureStr += "digest: " + digest
        signature = hmac.new(bytes(self.Secret),
                             bytes(signatureStr),
                             digestmod=hashlib.sha256).digest()
        result = base64.b64encode(signature)
        return result.decode(encoding='utf-8')

    def init_header(self, data):
        digest = self.hashlib_256(data)
        #print("digerst")
        sign = self.generateSignature(digest)
        authHeader = 'hmac username="%s", algorithm="%s", ' \
                     'headers="host date request-line digest", ' \
                     'signature="%s"' \
                     % (self.UserName, self.Algorithm, sign)
        #print("authHeader")
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Method": "POST",
            "Host": self.Host,
            "Date": self.Date,
            "Digest": digest,
            "Authorization": authHeader
        }
        return headers

    def get_body(self):
        audioData = self.imgRead((self.AudioPath))
        content = base64.b64encode(audioData).decode(encoding='utf-8')
        postdata = {
            "common": {"app_id": self.APPID},
            "business": self.BusinessArgs,
            "data": {
                "audio": content,
                "format": "audio/L16;rate=16000",
                "encoding":"raw",
            }
        }
        body = json.dumps(postdata)
        return body

    def call_url(self):
        interval=0
        a_dic={}
        print(self.url)
        try:
            body = self.get_body()
            headers = self.init_header(body)
            response = requests.post(self.url, data=body, headers=headers,timeout=10)
            status_code = response.status_code
            print(response) 
            interval = response.elapsed.total_seconds()
            if status_code!=200:
                code=status_code
                sid=''
                info='http status is not 200'
                delay=interval
            else:
                respData = json.loads(response.text)
                code = str(respData["code"])
                sid = str(respData["sid"])
                info=str(respData["message"])
                delay = interval
        except Exception as e:
            code = 500
            sid = ''
            info= "occur exception"
            delay = interval
        a_dic['code'] = code
        a_dic['info'] = info
        a_dic['time'] = delay
        print(json.dumps(a_dic))
        #print ([code, sid,info, delay])


if __name__ == '__main__':
    ##示例:  host="rest-api.xfyun.cn"域名形式 或者host="ip:port"形式
    host = sys.argv[1]
    #初始化类
    gClass=get_result(host)
    gClass.call_url()
