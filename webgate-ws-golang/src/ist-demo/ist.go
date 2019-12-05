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
	"net/http"
	"encoding/json"
	"errors"
)

var (

	hostUrl   = "wss://ist-api.xfyun.cn/v2/ist"
	apiKey    = "xxxxxxxxxxxxxxxxxxxxxxx"
	apiSecret = "xxxxxxxxxxxxxxxxxxxxxxx"
	appid     = "xxxxxxxx"
	file      = "0.pcm"
)

const (
	STATUS_FIRST_FRAME       = 0
	STATUS_CONTINUE_FRAME    = 1
	STATUS_LAST_FRAME        = 2
	HttpCodeSuccessHandshake = 101 //握手成功返回的httpcode
)

func main() {
	start()

}
type RespData struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Data    Data  `json:"data"`
	Sid string `json:"sid"`
}

type Data struct {
	Result *Result `json:"result"`
	Status int         `json:"status"`
}


type Decoder struct {
	results []*Result
}

func (d *Decoder)Decode(result *Result)  {
	if len(d.results)<=result.Sn{
		d.results = append(d.results,make([]*Result,result.Sn-len(d.results)+1)...)
	}
	if result.Pgs == "rpl"{
		for i:=result.Rg[0];i<=result.Rg[1];i++{
			d.results[i]=nil
		}
	}
	d.results[result.Sn] = result
}

func (d *Decoder)String()string  {
	var r string
	for _,v:=range d.results{
		if v== nil{
			continue
		}
		r += v.String()
	}
	return r
}

type Result struct {
	Ls bool `json:"ls"`
	Rg []int `json:"rg"`
	Sn int `json:"sn"`
	Pgs string `json:"pgs"`
	Ws []Ws `json:"ws"`
}

func (t *Result)String() string {
	var wss string
	for _,v:=range t.Ws{
		wss+=v.String()
	}
	return wss
}

type Ws struct {
	Bg int `json:"bg"`
	Cw []Cw `json:"cw"`
}

func (w *Ws)String()string  {
	var wss string
	for _,v:=range w.Cw{
		wss+=v.W
	}
	return wss
}

type Cw struct {
	Sc int `json:"sc"`
	W string `json:"w"`
}



func readResp(resp *http.Response) string {
	if resp == nil {
		return ""
	}
	b, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		panic(err)
	}
	return fmt.Sprintf("code=%d,body=%s", resp.StatusCode, string(b))
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

type WebsocketListener interface {
	OnMessage(data []byte)
	OnOpen(resp *http.Response)
	OnFail(err error)
	OnClose(err error)
}

type WebSocketClient struct {
	conn     *websocket.Conn
	Listener WebsocketListener
	Url      string
}

func (ws *WebSocketClient) Connect() {
	d := websocket.Dialer{}
	conn, rsp, err := d.Dial(ws.Url, nil)
	if err != nil {
		emsg := ""
		if rsp != nil {
			b, _ := ioutil.ReadAll(rsp.Body)
			emsg = fmt.Sprintf("emsg=%s,responseCode=%d,body=%s", err.Error(), rsp.StatusCode, string(b))
		}
		ws.Listener.OnFail(errors.New(emsg))
		return
	}
	ws.conn = conn
	ws.Listener.OnOpen(rsp)
	go ws.read()
}

func (ws *WebSocketClient) read() {
	for {
		_, msg, err := ws.conn.ReadMessage()
		if err != nil {
			ws.Listener.OnClose(err)
			return
		}
		ws.Listener.OnMessage(msg)
	}
}

func (ws *WebSocketClient) Send(v interface{}) error {
	return ws.conn.WriteJSON(v)
}

func (ws *WebSocketClient) Close() {
	ws.conn.Close()
}

/**
	OnMessage(data []byte)
	OnOpen(resp *http.Response)
	OnFail(err error)
	OnClose(err error)
 */
type IstWsClient struct {
	Url     string
	Decoder *Decoder
	client  *WebSocketClient
}

func (c *IstWsClient) OnOpen(resp *http.Response) {
	fmt.Println("success open connection")
}

func (c *IstWsClient) OnMessage(data []byte) {
	var resp RespData
	json.Unmarshal(data, &resp)
	if resp.Code != 0 {
		fmt.Println("error|", resp.Code, resp.Message,resp.Sid)
		return
	}
	c.Decoder.Decode(resp.Data.Result)
	fmt.Println("result", c.Decoder.String(),resp.Sid)
	if resp.Data.Status == 2{
		fmt.Println("fin====")
		c.client.Close()
	}
}

func (c *IstWsClient) OnFail(err error) {
	fmt.Println(err.Error())
}

func (c *IstWsClient) OnClose(err error) {
	fmt.Println(err.Error())
}

func start() {
	ist := &IstWsClient{
		Decoder:&Decoder{results:make([]*Result,0)},
	}
	client := &WebSocketClient{
		Listener: ist,
		Url:      assembleAuthUrl(hostUrl, apiKey, apiSecret),
	}
	ist.client = client
	client.Connect()
	//打开音频文件
	audioFile, err := os.Open(file)
	if err != nil {
		panic(err)
	}

	//开启协程，发送数据
	var frameSize = 1280               //每一帧的音频大小
	var intervel = 40 * time.Millisecond //发送音频间隔
	var status = STATUS_FIRST_FRAME      //音频的状态信息，标识音频是第一帧，还是中间帧、最后一帧
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
					"aue": "raw",
					"language":"zh_cn",
					"accent":"mandarin",
					"rate":"16000",
					"domain":"ist",
					"dwa": "wpgs",
					"ent":"ist",
					"eos": 30000000,
				},
				"data": map[string]interface{}{
					"status":   STATUS_FIRST_FRAME, //第一帧音频status要为 0
					"audio":    base64.StdEncoding.EncodeToString(buffer[:len]),
				},
			}
			client.Send(frameData)
			status = STATUS_CONTINUE_FRAME
		case STATUS_CONTINUE_FRAME:
			frameData := map[string]interface{}{
				"data": map[string]interface{}{
					"status": STATUS_CONTINUE_FRAME, // 中间音频status 要为1
					"audio":  base64.StdEncoding.EncodeToString(buffer[:len]),
				},
			}
			client.Send(frameData)
		case STATUS_LAST_FRAME:
			frameData := map[string]interface{}{
				"data": map[string]interface{}{
					"status": STATUS_LAST_FRAME, // 最后一帧音频status 一定要为2 且一定发送
					"audio":  base64.StdEncoding.EncodeToString(buffer[:len]),
				},
			}
			client.Send(frameData)
			goto end
		}
		//模拟音频采样间隔
		time.Sleep(intervel)
		count++


	}
end:
//获取返回的数据

	time.Sleep(2 * time.Second)
}
