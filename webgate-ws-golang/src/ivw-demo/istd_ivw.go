package main

import (
	"net/url"
	"fmt"
	"time"
	"strings"
	"encoding/base64"
	"crypto/hmac"
	"crypto/sha256"
	"io/ioutil"
	"github.com/gorilla/websocket"
	"os"
	"io"
	"sync"
	"strconv"
	"sync/atomic"
)

var (
	

	hostUrl = "ws://ws-api.xfyun.cn/v2/ivw"

	apiKey    = "xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
	apiSecret = "xxxxxxxxxxxxxxxxxxxx"
	//file      = "aclient_example/audio/dddd.pcm"
	file      = "test.pcm"
	appid     = "xxxxxxxx"
)

const (
	STATUS_FIRST_FRAME       = 0
	STATUS_CONTINUE_FRAME    = 1
	STATUS_LAST_FRAME        = 2
	HttpCodeSuccessHandshake = 101 //握手成功返回的httpcode
)

func main() {
	ivw()
}

var cidseed int32 = 0

func getCid() string {
	atomic.AddInt32(&cidseed, 1)
	return strconv.Itoa(int(cidseed))
}

func ivw() {
	st := time.Now()
	d := websocket.Dialer{
		HandshakeTimeout: 2 * time.Second,
	}
	//握手并建立websocket 连接
	conn, resp, err := d.Dial(assembleAuthUrl(hostUrl, apiKey, apiSecret), nil)
	if err != nil {
		panic(err)

		return
	}

	if resp.StatusCode != HttpCodeSuccessHandshake {
		b, _ := ioutil.ReadAll(resp.Body)
		fmt.Printf("handshake failed:message=%s,httpCode=%d\n", string(b), resp.StatusCode)
	}
	defer conn.Close()
	//打开音频文件

	fmt.Println("handshake:", time.Now().Sub(st))
	var lock sync.Mutex
	//开启协程，发送数据

	go func() {
		audioFile, err := os.Open(file)
		if err != nil {
			panic(err)
		}
		var frameSize = 1280              //每一帧的音频大小
		var intervel = 0 * time.Millisecond //发送音频间隔
		var status = STATUS_FIRST_FRAME     //音频的状态信息，标识音频是第一帧，还是中间帧、最后一帧
		var buffer = make([]byte, frameSize)
		var count = 0
		for {
			var len, err = audioFile.Read(buffer)
			if err != nil {
				if err == io.EOF { //文件读取完了，改变status = STATUS_LAST_FRAME
					status = STATUS_LAST_FRAME
				} else {
					panic(err)
				}
			}

			switch status {
			case STATUS_FIRST_FRAME: //发送第一帧音频，带business 参数
				frameData := map[string]interface{}{
					"common": map[string]interface{}{
						"app_id": appid, //appid 必须带上，只需第一帧发送
					},
					"business": map[string]interface{}{ //business 参数，只需一帧发送
						"id": 1,
						"bos": 100,
						"keyword": "叮咚叮咚",
						"threshold": 1400,
						"ent": "ivw",
						//"upload": true,
					},
					"data": map[string]interface{}{
						"status":   STATUS_FIRST_FRAME, //第一帧音频status要为 0
						"audio":    base64.StdEncoding.EncodeToString(buffer[:len]),
						"format":   "16000",
						"encoding": "raw",
					},
				}
				lock.Lock()
				conn.WriteJSON(frameData)
				lock.Unlock()
				status = STATUS_CONTINUE_FRAME
			case STATUS_CONTINUE_FRAME:
				frameData := map[string]interface{}{
					"common": map[string]interface{}{
						"app_id": appid, //appid 必须带上，只需第一帧发送
					},
					"data": map[string]interface{}{
						"status":   STATUS_CONTINUE_FRAME, // 中间音频status 要为1
						"audio":    base64.StdEncoding.EncodeToString(buffer[:len]),
						"format":   "16000",
						"encoding": "raw",
					},
				}
				lock.Lock()
				conn.WriteJSON(frameData)
				lock.Unlock()
			case STATUS_LAST_FRAME:
				frameData := map[string]interface{}{
					"common": map[string]interface{}{
						"app_id": appid, //appid 必须带上，只需第一帧发送
					},
					"data": map[string]interface{}{
						"status":   2, // 最后一帧音频status 一定要为2 且一定发送
						"audio":    base64.StdEncoding.EncodeToString(buffer[:len]),
						"format":   "16000",
						"encoding": "raw",
					},
				}
				lock.Lock()
				conn.WriteJSON(frameData)
				lock.Unlock()
				return
			}
			//模拟音频采样间隔
			time.Sleep(intervel)
			count++
			//if count==10{
			//	os.Exit(1)
			//}
		}
	}()

	//获取返回的数据
	_, msg, err := conn.ReadMessage()
	//err:=conn.ReadJSON(resp)
	if err != nil {
		fmt.Println("read message error:", err)
		return
	}
	fmt.Println("----------------")
	fmt.Printf("rsp:%s\n", string(msg))
	fmt.Println(time.Now().Sub(st))


}

type RespData struct {
	Sid     string `json:"sid"`
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    Data   `json:"data"`
}

type Data struct {
	Detail Result `json:"detail"`
	Status int    `json:"status"`
	Rsw    string `json:"rsw"`
}

type Result struct {
	Bos     int    `json:"bos"`
	Eos     int    `json:"eos"`
	Id      int    `json:"id"`
	Keyword string `json:"keyword"`
	Score   int    `json:"score"`
	Sst     string `json:"sst"`
	Ver     string `json:"ver"`
}

//创建鉴权url  apikey 即 hmac username
func assembleAuthUrl(hosturl string, apiKey, apiSecret string) string {
	ul, err := url.Parse(hosturl)
	if err != nil {
		fmt.Println(err)
	}
	//签名时间
	date := time.Now().UTC().Format(time.RFC1123)
	//参与签名的字段 host ,date, request-line
	signString := []string{"host: " + ul.Host, "date: " + date, "GET " + ul.Path + " HTTP/1.1"}
	//拼接签名字符串
	sgin := strings.Join(signString, "\n")
	//签名结果
	sha := HmacWithShaTobase64("hmac-sha256", sgin, apiSecret)
	//构建请求参数 authorization
	authUrl := fmt.Sprintf("hmac username=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey,
		"hmac-sha256", "host date request-line", sha)
	//将请求参数使用base64编码
	authorization := base64.StdEncoding.EncodeToString([]byte(authUrl))

	v := url.Values{}
	v.Add("host", ul.Host)
	v.Add("date", date)
	v.Add("authorization", authorization)
	//将编码后的字符串url encode后添加到url后面
	callurl := hosturl + "?" + v.Encode()
	return callurl
}

func HmacWithShaTobase64(algorithm, data, key string) string {
	mac := hmac.New(sha256.New, []byte(key))
	mac.Write([]byte(data))
	encodeData := mac.Sum(nil)
	return base64.StdEncoding.EncodeToString(encodeData)
}
