#!/usr/bin/env python 
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
        self.RequestUri = "/v2/ocr"
        #设置url
        if re.match("^\d", self.Host):
            #print(host)
            self.url="http://"+host+self.RequestUri
        else:
             #print(host)
             self.url="https://"+host+self.RequestUri
        self.HttpMethod = "POST"
        self.APPID = "5xxxx3"
        self.Algorithm = "hmac-sha256"
        self.HttpProto = "HTTP/1.1"
        self.UserName="xxxx98"
        self.Secret = "xxxxx4p"

        # 设置当前时间
        curTime_utc = datetime.datetime.utcnow()
        self.Date = self.httpdate(curTime_utc)
        #设置测试音频文件
        self.AudioPath="./ocr.jpg"
        self.BusinessArgs={
                "ent":"xx",
                "mode":"xx",
                "method":"xx",
                "sub":"ocr"
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
        #print(digest)
        sign = self.generateSignature(digest)
       #authHeader = 'hmac username="%s", algorithm="%s", ' \
        authHeader = 'api_key="%s",algorithm="%s", ' \
                     'headers="host date request-line digest", ' \
                     'signature="%s"' \
                     % (self.UserName, self.Algorithm, sign)
        #print(authHeader)
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
                "image": content,
            }
        }
        body = json.dumps(postdata)
        return body

    def call_url(self):
        try:
            body=self.get_body()
            headers=self.init_header(body)
            a_dic={}
            #print(self.url)
            response = requests.post(self.url, data=body, headers=headers,timeout=8)
            status_code = response.status_code
            interval = response.elapsed.total_seconds()
            if status_code!=200:
                code=status_code
                sid=''
                info= response.content
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
            info= e.message
            delay = interval
        a_dic['code'] = code
        a_dic['info'] = "%s" % str(info)
        a_dic['time'] = delay
        print(json.dumps(a_dic))
        #print ([code, sid,info, delay])


if __name__ == '__main__':
    ##示例:  host="rest-api.xfyun.cn"域名形式 或者host="ip:port"形式
    host = sys.argv[1]
    #初始化类
    gClass=get_result(host)
    gClass.call_url()
