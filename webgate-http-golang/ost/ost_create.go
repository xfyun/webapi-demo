package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"sync"
	"github.com/valyala/fasthttp"
	"time"
	"net/url"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"

)

var (
	waitGroup sync.WaitGroup
)
var (
	authEnable = true
)

const (
	//todo
	//支持的算法
	Algorithm = "hmac-sha256"
	//版本协议
	HttpProto = "HTTP/1.1"
	//商定的secret
	Secret = "xI65pqMN1M9v7QEI16ZvVQ"
	//Secret = "wzQUY2c3WW1ag8oTdT61Id89msf8xLl6"
)

func Usage() {
	fmt.Fprint(os.Stderr, "Usage of ", os.Args[0], ":\n")
	flag.PrintDefaults()
	fmt.Fprint(os.Stderr, "\n")
}

type CommonParam struct {
	AppID          string `json:"app_id"`
	UID            string `json:"uid"`
	DeviceID       string `json:"device_id"`
	DeviceImei     string `json:"device.imei"`
	DeviceImsi     string `json:"device.imsi"`
	DeviceMac      string `json:"device.mac"`
	DeviceOther    string `json:"device.other"`
	AuthAuthID     string `json:"auth.auth_id"`
	AuthAuthSource string `json:"auth.auth_source"`
	NetType        string `json:"net.type"`
	NetIsp         string `json:"net.isp"`
	AppVer         string `json:"app.ver"`
}

//定义请求的数据参数
type DataParam struct {
	Audio_url string `json:"audio_url"`
    Audio_src string `json:"audio_src"`
}


//外界请求信息
type RequestParam struct {
	Business map[string]interface{} `json:"business"` //业务参数
	Data     DataParam              `json:"data"`     //请求数据
	Common map[string]string `json:"common"` //公共参数
}

type ResponseParam struct {
	Code    int    `json:"code"`    //返回码
	Message string `json:"message"` //错误信息
	Sid  string      `json:"sid"`
	Data interface{} `json:"data"` //响应数据
}

func ostClient(serverUrl string, loops, timesleep int, reqData []byte, client *fasthttp.Client) {
	defer waitGroup.Done()
	for i := 1; i <= loops; i++ {

		//fmt.Printf("req is %s", reqData)
		request := fasthttp.AcquireRequest()
		response := fasthttp.AcquireResponse()
		request.SetConnectionClose()
		request.SetRequestURI(serverUrl)
		request.Header.SetContentType("application/json")
		request.SetBody(reqData)
		request.Header.SetMethod("POST")
	    api_key := "xx"
		//设置检验头
		u, _ := url.Parse(serverUrl)
		host := u.Host
		requestUri := u.RequestURI()
		if authEnable {
			assemblyRequestHeader(request, api_key, host, requestUri, reqData)
		}
		client.MaxConnsPerHost = 4096

		if err := client.Do(request, response); err != nil {
			fmt.Printf("client do error:%v\n", err)
		} else {
			fmt.Printf("head is:%+v\n", string(response.Header.Header()))
			fmt.Printf("body is:%+v\n", string(response.Body()))

			//查看状态码
			if response.StatusCode() != 200 {
				fmt.Printf("response status code is:%+v\n\n", response.StatusCode())
				fasthttp.ReleaseRequest(request)
				fasthttp.ReleaseResponse(response)
				return
			}

			var resp ResponseParam
			//解析内容
			err = json.Unmarshal(response.Body(), &resp)
			if err != nil {
				fmt.Printf("json unmarshal error:%+v\n", err)
				fasthttp.ReleaseRequest(request)
				fasthttp.ReleaseResponse(response)
				return
			}
			fasthttp.ReleaseRequest(request)
			fasthttp.ReleaseResponse(response)
		}
	}
	return
}

func main() {
	//定义命令行参数
	flag.Usage = Usage
	url  := flag.String("url", "http://ost-api.xfyun.cn/v2/ost/create", "connect server url")
	threads := flag.Int("thread", 1, "threads ")      //并发路数
	loops := flag.Int("loop", 1, "each thread loops") //循环次数
	timesle := flag.Int("time", 2000, "sleep time,millseconds")
	flag.Parse()
	var (
		APPID = "xxx"
	)
	var data = DataParam{
		Audio_url: "http://xxx",
		Audio_src: "http",
	}
	var bussinessParam = map[string]interface{}{
		"task_type":"xxx",
		"callback_url":"http://xxx",
		"callback_key":"xxx",
		"callback_secret":"xxx",
	}
	var commonParam = map[string]string{
		"app_id": APPID,
	}

	//封装请求
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
		Data:     data,
	}

	client := &fasthttp.Client{}

	reqData, _ := json.Marshal(reqParam)

	for i := 1; i <= *threads; i++ {

		waitGroup.Add(1)
		go ostClient(*url, *loops, *timesle, reqData, client)
	}

	return
}



func assemblyRequestHeader(req *fasthttp.Request, appid, host, uri string, body []byte) {
	req.Header.Set("Content-Type", "application/json")
	//设置请求头 其中Host Date 必须有
	req.Header.Set("Host", host)

	//date必须是utc时区，且不能和服务器时间相差300s
	currentTime := time.Now().UTC().Format(time.RFC1123)
	req.Header.Set("Date", currentTime)

	//对body进行sha256签名,生成digest头部，POST请求必须对body验证
	digest := "SHA-256=" + signBody(body)

	req.Header.Set("Digest", digest)

	//根据请求头部内容，生成签名
	sign := generateSignature(host, currentTime,
		"POST", uri, HttpProto, digest, Secret)
	 fmt.Printf("sign is:%s\n\n", sign)
	//组装Authorization头部
	authHeader := fmt.Sprintf(`hmac api_key="%s", algorithm="%s", headers="host date request-line digest", signature="%s"`, appid, Algorithm, sign)
	req.Header.Set("Authorization", authHeader)
}

func generateSignature(host, date, httpMethod, requestUri, httpProto, digest string, secret string) string {

	//不是request-line的话，则以 header名称,后跟ASCII冒号:和ASCII空格，再附加header值
	var signatureStr string
	if len(host) != 0 {
		signatureStr = "host: " + host + "\n"
	}
	signatureStr += "date: " + date + "\n"

	//如果是request-line的话，则以 http_method request_uri http_proto
	signatureStr += httpMethod + " " + requestUri + " " + httpProto + "\n"
	signatureStr += "digest: " + digest
	fmt.Printf("signature is:%s\n\n", signatureStr)
	return hmacsign(signatureStr, secret)
}

func hmacsign(data, secret string) string {
	fmt.Printf("data is:%v\n\n", data)
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write([]byte(data))
	encodeData := mac.Sum(nil)
	return base64.StdEncoding.EncodeToString(encodeData)
}

func signBody(data []byte) string {
	//进行sha256签名
	sha := sha256.New()
	sha.Write(data)
	encodeData := sha.Sum(nil)
	//经过base64转换
	return base64.StdEncoding.EncodeToString(encodeData)
}

