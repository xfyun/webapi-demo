package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"github.com/valyala/fasthttp"
	"net/url"
	"io/ioutil"
	"encoding/base64"
	"crypto/hmac"
	"crypto/sha256"
	"time"
)

const (
	Algorithm = "hmac-sha256" //支持的算法
	HttpProto = "HTTP/1.1"    //版本协议
	Secret    = "xxxxxxx"     //商定的secret
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
	Image string `json:"image"` //图片数据
}

//外界请求信息
type RequestParam struct {
	Common   map[string]string      `json:"common"`   //应用id
	Business map[string]interface{} `json:"business"` //业务参数
	Data     DataParam              `json:"data"`     //请求数据
}

type ResponseParam struct {
	Code    int         `json:"code"`    //返回码
	Message string      `json:"message"` //错误信息
	Sid     string      `json:"sid"`
	Data    interface{} `json:"data"` //响应数据
}

//测试唤醒词引擎
func ipbrClient(serverUrl string, reqData []byte, client *fasthttp.Client) {
	request := fasthttp.AcquireRequest()
	response := fasthttp.AcquireResponse()
	request.SetConnectionClose()
	request.SetRequestURI(serverUrl)
	request.Header.SetContentType("application/json")
	request.SetBody(reqData)
	request.Header.SetMethod("POST")
	username := "xxxxx"
	//设置检验头
	u, _ := url.Parse(serverUrl)
	host := u.Host
	requestUri := u.RequestURI()
	assemblyRequestHeader(request, username, host, requestUri, reqData)
	client.MaxConnsPerHost = 4096
	defer fasthttp.ReleaseRequest(request)
	defer fasthttp.ReleaseResponse(response)
	if err := client.Do(request, response); err != nil {
		fmt.Printf("client do error:%v\n", err)
	} else {
		fmt.Printf("head is:%+v\n", string(response.Header.Header()))
		fmt.Printf("body is:%+v\n", string(response.Body()))
		//查看状态码
		if response.StatusCode() != 200 {
			fmt.Printf("response status code is:%+v\n\n", response.StatusCode())
			return
		}
		var resp ResponseParam
		//解析内容
		err = json.Unmarshal(response.Body(), &resp)
		if err != nil {
			fmt.Printf("json unmarshal error:%+v\n", err)
			return
		}
	}
	return
}

func main() {
	//定义命令行参数
	flag.Usage = Usage
	url := flag.String("url", "https://typre.xfyun.cn/v2/pre", "connect server url")
	res := flag.String("res", "11.jpg", "res path")
	flag.Parse()
	audioFile1 := *res
	audio1, err := ioutil.ReadFile(audioFile1)
	if err != nil {
		fmt.Printf("read audiofile:%s err:%v\n", err)
		return
	}
	contentByte := base64.StdEncoding.EncodeToString(audio1)
	var APPID = "xxxx"
	var data1 = DataParam{
		Image: contentByte,
	}
	var bussinessParam = map[string]interface{}{
		"ent":    "ipbr",
		"cover_index":"0",
		"retri_type":"1",
	}
	var commonParam = map[string]string{
		"app_id": APPID,
	}
	//封装请求
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
		Data:     data1,
	}
	client := &fasthttp.Client{}
	reqData, _ := json.Marshal(reqParam)
	ipbrClient(*url, reqData, client)
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
	authHeader := fmt.Sprintf(`hmac username="%s", algorithm="%s", headers="host date request-line digest", signature="%s"`, appid, Algorithm, sign)
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
