package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
	"time"
)

const (
	ApiKey="xxxx"
	SecretKey="xxx"
)

var appid = "xxx"

type RequestParam struct {
	Common   map[string]string      `json:"common"`   //通用参数
	Business map[string]interface{} `json:"business"` //业务参数
	Data     string              `json:"data,omitempty"`     //请求数据
}

func istRequest() {

	var bussinessParam = map[string]interface{}{
		"busin": "refrigerator",
		"scene": "noise",
		"gender": "male",
		"age": "teenager",
		"pinyin": "da3_kai1_kong1_tiao4",
		"chinese": "打开空调",
		"vocabulary": "xxx",
		"mode": "main",
	}
	var commonParam = map[string]string{
		"app_id": appid,
		"mac_net": "xxx",
		"mac_wifi": "xxx",
		"device_id": "xxx",
	}
	dat, err := ioutil.ReadFile("./example/audio.pcm")
	if err != nil {
		panic(err)
	}
	//var Data = base64.StdEncoding.EncodeToString([]byte{11, 2, 2})
	var Data = base64.StdEncoding.EncodeToString(dat)
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
		Data:     Data,
	}
	body, _ := json.Marshal(&reqParam)
	fmt.Println("-------------------", string(body))
	req, err := http.NewRequest("POST", "https://aiot-evo.xfyun.cn/individuation/asr/upload", strings.NewReader(string(body)))
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type","application/raw;16000")
	assemblyRequestHeader(req, "aiot-evo.xfyun.cn", "/individuation/asr/upload")
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}

func DownLoad( ) {
	//上传返回的index
	var index = "ctm00010001@hu1724b2026bb0212902"
	var bussinessParam = map[string]interface{}{
		"index": index,
	}
	var commonParam = map[string]string{
		"app_id": appid,
		"mac_net": "xxx",
		"mac_wifi": "xxx",
		"device_id": "xxx",
	}
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
	}
	body, _ := json.Marshal(&reqParam)
	req, err := http.NewRequest("POST", "https://aiot-evo.xfyun.cn/individuation/asr/download", strings.NewReader(string(body)))
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type","application/raw;16000")
	assemblyRequestHeader(req, "aiot-evo.xfyun.cn", "/individuation/asr/download")
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}

func assemblyRequestHeader(req *http.Request, host, path string) {
	//设置请求头 其中Host Date 必须有
	req.Header.Set("Host", host)
	//date必须是utc时区，且不能和服务器时间相差300s
	currentTime := time.Now().UTC().Format(time.RFC1123)
	req.Header.Set("Date", currentTime)
	//对body进行sha256签名,生成digest头部，POST请求必须对body验证
	//digest := signBody(body)
	//req.Header.Set("Digest", "SHA-256="+digest)
	//根据请求头部内容，生成签名
	sign := generateSignature(req.Header.Get("Host"), req.Header.Get("Date"),
		"POST", path, "HTTP/1.1", SecretKey)
	//组装Authorization头部
	var authHeader = `hmac api_key="` + ApiKey + `", algorithm="hmac-sha256", headers="host date request-line", signature="` + sign + `"`
	fmt.Println(authHeader)
	req.Header.Set("Authorization", authHeader)
}

func generateSignature(host, date, httpMethod, requestUri, httpProto, secret string) string {

	//不是request-line的话，则以 header名称,后跟ASCII冒号:和ASCII空格，再附加header值
	var signatureStr string
	if len(host) != 0 {
		signatureStr = "host: " + host + "\n"
	}
	signatureStr += "date: " + date + "\n"

	//如果是request-line的话，则以 http_method request_uri http_proto
	signatureStr += httpMethod + " " + requestUri + " " + httpProto
	//signatureStr += "digest: " + digest
	fmt.Println(signatureStr)
	return hmacsign(signatureStr, secret)
}

func hmacsign(data, secret string) string {
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
func main() {
	istRequest()
	DownLoad()
}
