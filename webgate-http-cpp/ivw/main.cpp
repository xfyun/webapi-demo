//
//  main.cpp
//  webgate
//
//  Created by lucky on 2019/8/1.
//  Copyright © 2019 lucky. All rights reserved.
//

#include <iostream>
#include "base64.h"
#include <openssl/hmac.h>
#include <openssl/evp.h>
#include "time.h"
#include <curl/curl.h>
#include<streambuf>
#include<fstream>
#include<sstream>
#include <json/json.h>
#include <cstring>
class WebgateHttp
{
public:
    WebgateHttp(std::string host);
    //base64编码
    std::string Base64Encode(const std::string src);
    //sha256
    std::string GetSha256(const std::string src);
    //hmac-sha256
    std::string GetHmacSha256(const char * key, unsigned int key_length,const char * input, unsigned int input_length);
    //读取文件
    std::string ReadFile(std::string fileName);
    //获取gmt时间
    std::string GetGMTTime(void);
    //获取digest和authorization
    void SetDigestAndAuthorization(std::string body);
    //接收到数据的处理
    static size_t receive_data(void *contents, size_t size, size_t nmemb, void *stream);
    //服务调用
    void CallUrl();
private:
    std::string             m_host; //主机地址
    std::string             m_requestUri="/v2/ivw";
    std::string             m_strHostUrl;               // 云端合成请求url
    std::string             m_httpMethod="POST"; //调用的http方法
    std::string             m_appId="5xxxx"; //appid
    std::string             m_algorithm="hmac-sha256"; //采用的算法
    std::string             m_httpProto="HTTP/1.1"; //http协议版本
    std::string             m_userName="48xxxxxxxxxxxxxxxxxxxxxxx"; //用户名
    std::string             m_secret="Tx7xxxxxxxxxxxxxxxxxxxx"; //secret
    std::string             m_gmtTime; //gmt时间
    std::string             m_audioFile="./dddd.pcm";
    std::string             m_authorization; //authorization信息
    std::string             m_digest; //body签名信息
};


WebgateHttp::WebgateHttp(std::string host)
{
    m_host=host;
    m_strHostUrl="https://"+host+m_requestUri;
    m_gmtTime=GetGMTTime();
//    m_gmtTime="Fri, 09 Aug 2019 07:06:46 GMT";
}


//base64编码
std::string WebgateHttp::Base64Encode(const std::string input)
{
    static std::string encoded;
    if (!Base64::Encode(input, &encoded)) {
        std::cout << "Failed to encode input string" << std::endl;
        return "";
    }
    return encoded;
}

#include <openssl/sha.h>
//sha256
std::string WebgateHttp::GetSha256(const std::string str)
{
    // 调用sha256哈希
    unsigned char mdStr[33] = {0};
    SHA256((const unsigned char *)str.c_str(), str.length(), mdStr);
    
    // 哈希后的字符串
    std::string encodedStr = std::string((const char *)mdStr);
    return encodedStr;
}

std::string WebgateHttp::GetGMTTime(void)
{
    time_t now = time(nullptr);
    tm* gmt = gmtime(&now);
    
    // http://en.cppreference.com/w/c/chrono/strftime
    // e.g.: Sat, 22 Aug 2015 11:48:50 GMT
    //       5+   3+4+   5+   9+       3   = 29
    const char* fmt = "%a, %d %b %Y %H:%M:%S GMT";
    char tstr[30];
    
    strftime(tstr, sizeof(tstr), fmt, gmt);
    return tstr;
}

//读取文件
std::string WebgateHttp::ReadFile(std::string fileName)
{
    std::ifstream t(fileName);
    std::stringstream buffer;
    buffer << t.rdbuf();
    std::string contents(buffer.str());
    return contents;
}

std::string WebgateHttp::GetHmacSha256(const char *key, unsigned int key_length,
    const char *input, unsigned int input_length)
{
    const void* pKey = (const void*)key;
    const unsigned char* pData = (const unsigned char*)input;
    unsigned char result[EVP_MAX_MD_SIZE];
    unsigned int len;

    HMAC(EVP_sha256(), pKey, key_length, pData, input_length, result, &len);

    std::string tmp((const char*)result, len);

    return tmp;
}
void WebgateHttp::SetDigestAndAuthorization(std::string body)
{
    // 1. 获取gmt时间
    std::string gmtTime=m_gmtTime;

    std::cout << "gmtTime is:" << gmtTime << std::endl;
    // 2. 生成签名
    //body做签名
    std::string digest;
    std::string bodySha256=GetSha256(body);
    digest="SHA-256="+Base64Encode(bodySha256);
    m_digest=digest;
    
    //生成签名串
    std::string strSign;
    strSign =   "host: " + m_host +
    "\ndate: " + gmtTime +
    "\n" + m_httpMethod + " "+m_requestUri+" "+m_httpProto+"\n"+"digest: "+digest;;
    
    // 3. 对签名H256编码
    std::string signature=GetHmacSha256(m_secret.c_str(), m_secret.length(), strSign.c_str(), strSign.length());
    
    //4、签名base64编码
    std::string signatrueBase64=Base64Encode(signature);
    std::cout << "strSign base64 is:"+signatrueBase64 << std::endl;
    
    //构建请求参数
    std::string strOrigin;
    strOrigin += "api_key=";
    strOrigin += "\"" + m_userName + "\"";
    strOrigin += ",algorithm=";
    strOrigin += "\"hmac-sha256\"";
    strOrigin += ",headers=";
    strOrigin += "\"host date request-line digest\"";
    strOrigin += ",signature=";
    strOrigin += "\"" + signatrueBase64 + "\"";

    m_authorization=strOrigin;
    return ;
}

 size_t WebgateHttp::receive_data(void *contents, size_t size, size_t nmemb, void *stream)
{
    std::string *str = (std::string*)stream;
    (*str).append((char*)contents, size*nmemb);
    return size * nmemb;
}

void WebgateHttp::CallUrl()
{
    try
    {
        CURL *pCurl = NULL;
        CURLcode res;
        // In windows, this will init the winsock stuff
        curl_global_init(CURL_GLOBAL_ALL);
        
        // get a curl handle
        pCurl = curl_easy_init();
        if (NULL != pCurl)
        {
            //构造body
            std::string content=ReadFile(m_audioFile);
            std::string contentBase64=Base64Encode(content);
            //构建json
            
            Json::Value root(Json::objectValue);
            Json::Value &common    = root["common"];
            Json::Value &business  = root["business"];
            Json::Value &data      = root["data"];
            common["app_id"] = m_appId;               // appid
            business["ent"]  = "ivw";
            business["keyword"]="叮咚叮咚";
            business["threshold"]="1314";
            data["audio"]=contentBase64;
            data["format"]="16000";
            data["encoding"]="raw";

            //转为无格式的json
            std::string out;
            Json::FastWriter json2str;
            out = json2str.write(root);
            
            // 设置超时时间为5秒
            curl_easy_setopt(pCurl, CURLOPT_TIMEOUT, 20);
            
            // First set the URL that is about to receive our POST.
            // This URL can just as well be a
            // https:// URL if that is what should receive the data.
            std::cout << m_strHostUrl << std::endl;
            curl_easy_setopt(pCurl, CURLOPT_URL, m_strHostUrl.c_str());
            
            SetDigestAndAuthorization(out);
            
            std::string methodHeader="Method:"+m_httpMethod;
            std::string hostHeader="Host:"+m_host;
            std::string dateHeader="Date:"+m_gmtTime;
            std::string authorizationHeader="Authorization:"+m_authorization;
            std::string digestHeader="Digest:"+m_digest;
            std::cout << "method is:"+m_httpMethod<< std::endl;
            std::cout << "Host is:"+m_host<< std::endl;
            std::cout << "Date is:"+m_gmtTime<< std::endl;
            std::cout << "Authorization is:"+authorizationHeader<< std::endl;
            std::cout << "Digest is:"+digestHeader<< std::endl;
            
            //定义请求头
            curl_slist *plist=curl_slist_append(NULL, "Content-Type:application/json");
            plist=curl_slist_append(plist,"Accept:application/json");
            plist=curl_slist_append(plist,methodHeader.c_str());
            plist=curl_slist_append(plist,hostHeader.c_str());
            plist=curl_slist_append(plist,dateHeader.c_str());
            plist=curl_slist_append(plist,authorizationHeader.c_str());
            plist=curl_slist_append(plist,digestHeader.c_str());
            curl_easy_setopt(pCurl, CURLOPT_HTTPHEADER, plist);
            
            
            std::string postret;
            curl_easy_setopt(pCurl,CURLOPT_WRITEDATA,&postret);
            curl_easy_setopt(pCurl,CURLOPT_WRITEFUNCTION,receive_data);
            // 设置要POST的JSON数据
            curl_easy_setopt(pCurl,CURLOPT_POST, 1L); //设置为非0表示本次操作为POST
            //curl_easy_setopt(pCurl, CURLOPT_VERBOSE, 1); //设置为非0在执行时打印请求信息
            curl_easy_setopt(pCurl, CURLOPT_POSTFIELDS, out.c_str());
            
            // Perform the request, res will get the return code
            res = curl_easy_perform(pCurl);
            // Check for errors
            if (res != CURLE_OK)
            {
                curl_easy_cleanup(pCurl);
                printf("curl_easy_perform() failed:%s\n", curl_easy_strerror(res));
                return;
            }
            
            //获取结果
//            std::cout << "res is:"+postret << std::endl;
            //解析结果
            Json::Reader reader;
            Json::Value rootParse;
            
            if (reader.parse(postret, rootParse))  // reader将Json字符串解析到root，root将包含Json里所有子元素
            {
                if (rootParse["code"].isNull()){
                    curl_easy_cleanup(pCurl);
                    std::cout << "errMsg is:"<<postret << std::endl;
                    return;
                }
                int code = rootParse["code"].asInt();
                std::string message = rootParse["message"].asString();
                std::string sid = rootParse["sid"].asString();
                std::string data = rootParse["data"].toStyledString();
                if (code!=0)
                {
                    std::cout << "errMsg is:"<<message << std::endl;
                }else{
                     std::cout << "sid is:"<<sid << std::endl;
                    std::cout << "data is:"<<data << std::endl;
                }
            }
            // always cleanup
            curl_easy_cleanup(pCurl);
        }
        curl_global_cleanup();
    }
    catch (std::exception &ex)
    {
        printf("curl exception %s.\n", ex.what());
    }
    return;
}

int main(int argc, const char * argv[]) {
    std::string host = "rest-api.xfyun.cn";
    WebgateHttp w=WebgateHttp(host);
    w.CallUrl();
    return 0;
}

