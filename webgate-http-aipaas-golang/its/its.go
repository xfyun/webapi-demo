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
	"net/url"
	//"net"
)

const (
	ApiKey    = "xx"
	SecretKey = "xx"
)

var appid = "xx"

type RequestParam struct {
	Header    map[string]string      `json:"header"`    //通用参数
	Parameter map[string]interface{} `json:"parameter"` //业务参数
	Payload   map[string]interface{} `json:"payload"`   //请求数据
}

//upload
func Request() {

	var service_id = map[string]interface{}{
		"from": "cn",
		"to":   "ja",
	}
	var parameter = map[string]interface{}{
          "s67c9c79d":service_id,
	}
	var header = map[string]string{
		"app_id": appid,
	}

	//var Data = base64.StdEncoding.EncodeToString(dat)
	var input = map[string]interface{}{
		"status":3,
		"text":"5L2g5aW9",
	}
	var payload = map[string]interface{}{
		"input1": input,
	}
	var reqParam = RequestParam{
		Header:    header,
		Parameter: parameter,
		Payload:   payload,
	}
	body, _ := json.Marshal(&reqParam)
	fmt.Println("-------------------", string(body))
	server_url := "https://api.xf-yun.com/v1/private/s67c9c79d"
	req, err := http.NewRequest("POST", server_url, strings.NewReader(string(body)))
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type", "application/json")
	u, _ := url.Parse(server_url)
	host := u.Host
	requestUri := u.RequestURI()
	assemblyRequestHeader("POST", req, host, requestUri)
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}

func assemblyRequestHeader(method string, req *http.Request, host, path string) {
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
		method, path, "HTTP/1.1", SecretKey)
	//组装Authorization头部
	var authHeader = `api_key="` + ApiKey + `", algorithm="hmac-sha256", headers="host date request-line", signature="` + sign + `"`
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
	Request()
}
