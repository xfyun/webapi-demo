package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"mime/multipart"

	"github.com/valyala/fasthttp"

	"net/textproto"
	"os"
	"sync"
)

var (
	waitGroup sync.WaitGroup
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
	//Text     string `json:"text"`     //文本数据
	//Image    string `json:"image"`    //图片数据
	Audio string `json:"audio"` //音频数据
	//VIDEO    string `json:"video"`     //视频数据
	Format   string `json:"format"`   //数据的编码格式 auf
	Encoding string `json:"encoding"` //数据的压缩格式 au
}

//外界请求信息
type RequestParam struct {
	Common map[string]string `json:"common"` //应用id
	//	Call         string            `json:"call"`    //请求服务类型
	//	From         string            `json:"from"`    //服务来源
	//	Version      string            `json:"version"` //服务版本号
	//	Sync         bool              `json:"sync"`    //是否同步返回结果
	Business map[string]interface{} `json:"business"` //业务参数
	Data     DataParam              `json:"data"`     //请求数据
}

type ResponseParam struct {
	Code    int    `json:"code"`    //返回码
	Message string `json:"message"` //错误信息
	Uid  string      `json:"uid"`
	Sid  string      `json:"sid"`
	Data interface{} `json:"data"` //响应数据
}

var (
	requestUri = "/v2/ocr"
	url        = "https://rest-api.xfyun.cn/v2/ocr"
	host       = "rest-api.xfyun.cn"
)

func main() {
	formdataDemo(url)
}

func formdataDemo(testURL string) error {
	b := &bytes.Buffer{}
	w := multipart.NewWriter(b)
	fields := map[string]string{
		"parameters": "common.app_id=xxx&business.ent=xx&business.sub=ocr&business.mode=xx&business.method=dynamic",
	}

	for k, v := range fields {
		_ = w.WriteField(k, v)
	}

	//设置data ，可以传多个，多个data 的fileName 要不一样，但是name要一样
	dh := textproto.MIMEHeader{}
	dh.Set("Content-Disposition",
		fmt.Sprintf(`form-data; name="payload.image.data"; filename="fingerocr.jpg"`))
	dh.Set("Content-Type", "application/octet-stream")
	ww, err := w.CreatePart(dh)
	if err != nil {
		panic(err)
	}
	imageFile0 := "./res/fingerocr.jpg"
	image0, err := ioutil.ReadFile(imageFile0)
	ww.Write(image0)
	w.Close()

	// 发送请求
	client := &fasthttp.Client{}
	request := fasthttp.AcquireRequest()
	response := fasthttp.AcquireResponse()
	request.SetConnectionClose()
	request.SetRequestURI(testURL)
	request.Header.SetContentType(w.FormDataContentType())
	request.SetBody(b.Bytes())
	request.Header.SetMethod("POST")
	username := "xxxx"
	//设置检验头
	assemblyRequestHeader(request, username, host, requestUri, b.Bytes())
	if err := client.Do(request, response); err != nil {
		fmt.Printf("client do error:%v\n", err)
		return err
	}
	//查看状态码
	if response.StatusCode() != 200 {
		fmt.Printf("response status code is:%+v\n\n", response.StatusCode())
		fmt.Printf("response body is:%+v\n\n", string(response.Body()))
		fasthttp.ReleaseRequest(request)
		fasthttp.ReleaseResponse(response)
		return err
	}
	var resp ResponseParam
	//解析内容
	err = json.Unmarshal(response.Body(), &resp)
	if err != nil {
		fmt.Printf("json unmarshal error:%+v\n", err)
		fasthttp.ReleaseRequest(request)
		fasthttp.ReleaseResponse(response)
		return err
	}
	fmt.Printf("resp is:%+v\n\n", string(response.Body()))
	fasthttp.ReleaseRequest(request)
	fasthttp.ReleaseResponse(response)
	return nil
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
