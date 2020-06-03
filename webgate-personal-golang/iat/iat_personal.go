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
)

const (
	ApiKey="xxxx"
	SecretKey="xxx"
)

var appid = "xx"

type RequestParam struct {
	Common   map[string]string      `json:"common"`   //通用参数
	Business map[string]interface{} `json:"business"` //业务参数
	Data     string              `json:"data,omitempty"`     //请求数据
}

//upload
func uploadRequest() {

	var bussinessParam = map[string]interface{}{
		"type": "contact",
	}
	var commonParam = map[string]string{
		"app_id": appid,
		"uid": "7049218823",//从venus获取的uid
	}
	dat, err := ioutil.ReadFile("demo/iat/contact")
	if err != nil {
		panic(err)
	}
	var Data = base64.StdEncoding.EncodeToString(dat)
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
		Data:     Data,
	}
	body, _ := json.Marshal(&reqParam)
	fmt.Println("-------------------", string(body))
	server_url:="https://evo.xfyun.cn/individuation/iat/upload"

	req, err := http.NewRequest("POST", server_url, strings.NewReader(string(body)))
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type","application/raw;16000")
	u, _ := url.Parse(server_url)
	host := u.Host
	requestUri := u.RequestURI()
	assemblyRequestHeader("POST",req, host, requestUri)
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}

func deleteIat( ) {
	//上传返回的index
	var bussinessParam = map[string]interface{}{
		"type": "content",
	}
	var commonParam = map[string]string{
		"app_id": appid,
		"uid": "7049218823",
	}
	var reqParam = RequestParam{
		Common:   commonParam,
		Business: bussinessParam,
	}
	body, _ := json.Marshal(&reqParam)
	server_url:="https://evo.xfyun.cn/individuation/iat/content"

	req, err := http.NewRequest("DELETE", server_url, strings.NewReader(string(body)))
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type","application/json")
	u, _ := url.Parse(server_url)
	host := u.Host
	requestUri := u.RequestURI()
	assemblyRequestHeader("DELETE",req, host, requestUri)
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}


func downloadIat( ) {
	
	server_url:="http://evo.xfyun.cn/individuation/iat/content?app_id=5a1e585b&uid=7049218823&type=contact"

	req, err := http.NewRequest("GET", server_url,nil)
	if err != nil {
		fmt.Println("err is is ", err)
	}
	client := &http.Client{}
	req.Header.Set("Content-Type","application/json")
	u, _ := url.Parse(server_url)
	host := u.Host
	requestUri := u.RequestURI()
	assemblyRequestHeader("GET",req, host, requestUri)
	resp, err := client.Do(req)
	if err != nil {
		fmt.Println("err is", err)
		return
	}
	respBody, _ := ioutil.ReadAll(resp.Body)
	fmt.Println(string(respBody))
}





func assemblyRequestHeader(method string ,req *http.Request, host, path string) {
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
	//uploadRequest()
	//downloadIat()
	deleteIat()
}
