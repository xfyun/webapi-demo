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
import time

path_pwd = os.path.split(os.path.realpath(__file__))[0]
os.chdir(path_pwd)


class get_result(object):
    def __init__(self,host):
        # 以下为POST请求
        self.Host = host
        self.RequestUriCreate = "/v2/ost/create"
        self.RequestUriQuery = "/v2/ost/query"
        #设置url
        if re.match("^\d", self.Host):
            #print(host)
            self.urlCreate = "http://"+host+self.RequestUriCreate
            self.urlQuery = "http://"+host+self.RequestUriQuery
        else:
            #print(host)
            self.urlCreate = "https://"+host+self.RequestUriCreate
            self.urlQuery = "https://" + host + self.RequestUriQuery
        self.HttpMethod = "POST"
        self.APPID = "xxx"
        self.Algorithm = "hmac-sha256"
        self.HttpProto = "HTTP/1.1"
        self.UserName = "xxx"
        self.Secret = "xxx"

        # 设置当前时间
        cur_time_utc = datetime.datetime.utcnow()
        self.Date = self.httpdate(cur_time_utc)
        #设置测试音频文件
        self.BusinessArgsCreate = {
                "task_type": "test_type",
                "callback_url":"http://xxx.xxx.xxx.xxx:8000/ost"
            }

    def img_read(self, path):
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

    def generateSignature(self, digest, uri):
        signature_str = "host: " + self.Host + "\n"
        signature_str += "date: " + self.Date + "\n"
        signature_str += self.HttpMethod + " " + uri \
                        + " " + self.HttpProto + "\n"
        signature_str += "digest: " + digest
        #print signature_str
        signature = hmac.new(bytes(self.Secret),
                             bytes(signature_str),
                             digestmod=hashlib.sha256).digest()
        result = base64.b64encode(signature)
        return result.decode(encoding='utf-8')

    def init_header(self, data, uri):
        digest = self.hashlib_256(data)
        sign = self.generateSignature(digest, uri)
        auth_header = 'api_key="%s",algorithm="%s", ' \
                     'headers="host date request-line digest", ' \
                     'signature="%s"' \
                     % (self.UserName, self.Algorithm, sign)
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json",
            "Method": "POST",
            "Host": self.Host,
            "Date": self.Date,
            "Digest": digest,
            "Authorization": auth_header
        }
        return headers

    def get_create_body(self):
        post_data = {
            "common": {"app_id": self.APPID},
            "business": self.BusinessArgsCreate,
            "data": {
                "audio_src": "http",
                "audio_url": "http://xxx/testaudio.pcm"
            }
        }
        body = json.dumps(post_data)
        return body

    def get_query_body(self, task_id):
        post_data = {
            "common": {"app_id": self.APPID},
            "business": {
                "task_id": task_id,
            },
        }
        body = json.dumps(post_data)
        return body

    def call(self, url, body, headers):

        interval = 0
        try:
            a_dic = {}
            response = requests.post(url, data=body, headers=headers, timeout=8)
    	    #print(response.content)
            status_code = response.status_code
            interval = response.elapsed.total_seconds()
            if status_code != 200:
                code = status_code
                sid = ''
                info = response.content
                delay = interval
            else:
                resp_data = json.loads(response.text)
                code = str(resp_data["code"])
                sid = str(resp_data["sid"])
                info = str(resp_data["message"])
                delay = interval
        except Exception as e:
            code = 500
            info = e.message
            delay = interval
        a_dic['code'] = code
        a_dic['info'] = "%s" % str(info)
        a_dic['time'] = delay
        if 'create' in url and code != '0':
            try:
                print(json.dumps(a_dic))
            except:
                a_dic['code'] = 500
                a_dic['info'] = "exec script err! detail:%s" % str(info)
                a_dic['time'] = 0
                print(json.dumps(a_dic))
        elif 'create' in url and code == '0':        
            if 'data' in resp_data and 'task_id' in str(resp_data["data"]):
                return resp_data["data"]["task_id"]
        elif 'query' in url:
            print(json.dumps(a_dic))
   
    def call_url(self):
            body = self.get_create_body()
            headers_create = self.init_header(body, self.RequestUriCreate)
            task_id = gClass.call(self.urlCreate, body, headers_create)
            if task_id is not None:
                query_body = self.get_query_body(task_id)
                headers_query = self.init_header(body, self.RequestUriQuery)
                gClass.call(self.urlQuery, query_body, headers_query)


if __name__ == '__main__':
    ##示例:  host="rest-api.xfyun.cn"域名形式 或者host="ip:port"形式
    host = "ost-api.xfyun.cn"
    #初始化类
    gClass = get_result(host)
    gClass.call_url()
