package main

import (
	"encoding/base64"
	"fmt"
	"github.com/gorilla/websocket"
	"io/ioutil"
	"log"
	"net/url"
	"strings"
	"time"
	"encoding/json"
	"os"
	"flag"
	"hash"
	"crypto/sha256"
	"crypto/sha512"
	"crypto/sha1"
	"crypto/hmac"
)

var text = ` 456 hello ,`
var first = `123`
var file = flag.String("f", "test.txt", "file")

func main() {
	 ws()

}

func ReadFileBytes(f string) ([]byte, error) {
	file, err := os.Open(f)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	b, err := ioutil.ReadAll(file)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func ws() {

client := NewClient("ws://tts-api.xfyun.cn", "/v2/tts", "xxxxxxxxxxx", "xxxxxxxxxx")

	defer client.Close()
	go func() {
		client.sendLast()
	}()

	//读取响应数据
	client.Read()
}

//@u:u url such as ws://10.1.87.70
//@path such as /wsapi/iat
//@uname
//@secret
// 创建带有签名的url
func CreateCallUrl(prto, path, appKey, secretKey string) string {
	//签名时间
	date := time.Now().UTC().Format(time.RFC1123)
	fmt.Println(date)
	//date = "Thu, 18 Jul 2025 07:36:19 UTC"
	hostIdx := strings.Index(prto, "//")
	signString := []string{"host: " + prto[hostIdx+2:], "date: " + date, "GET " + path + " HTTP/1.1"}
	//签名字符串

	sgin := strings.Join(signString, "\n")
	//签名结果
	sha := HmacWithShaTobase64("hmac-sha256", sgin, secretKey)
	//构建请求参数 此时不需要urlencoding
	urls := fmt.Sprintf("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", appKey,
		"hmac-sha256", "host date request-line", sha)
	//将请求参数使用base64编码
	urls = base64.StdEncoding.EncodeToString([]byte(urls))

	v := url.Values{}
	v.Add("host", prto[hostIdx+2:])
	v.Add("date", date)
	v.Add("authorization", urls)
	//将编码后的字符串url encode后添加到url后面
	callurl := prto + path + "?" + v.Encode()
	log.Println(callurl)
	return callurl
}
var hashAlgorithmMap = map[string]func() hash.Hash{
	"hmac-sha256": sha256.New,
	"hmac-sha512": sha512.New,
	"hmac-sha1":   sha1.New,
}

func HmacWithShaTobase64(algorithm, data, key string) string {
	mac := hmac.New(hashAlgorithmMap[algorithm], []byte(key))
	mac.Write([]byte(data))
	encodeData := mac.Sum(nil)
	return base64.StdEncoding.EncodeToString(encodeData)
}

func SupportAlgorithm(algorithm string) bool {
	if _, ok := hashAlgorithmMap[algorithm]; !ok {
		return false
	}
	return true
}

type Client struct {
	*websocket.Conn
}

func NewClient(u, path, uname, secret string) (client *Client) {
	t:=time.Now()
	d := websocket.Dialer{
		HandshakeTimeout: 2*time.Second,
	//	TLSClientConfig:&tls.Config{InsecureSkipVerify:true},
	}
	conn, resp, err := d.Dial(CreateCallUrl(u, path, uname, secret), nil)
	fmt.Println("handshake cost:",time.Since(t))
	if err != nil || resp.StatusCode != 101 {
		fmt.Println(err)
		b, _ := ioutil.ReadAll(resp.Body)
		panic(string(b))
	}
	client = &Client{
		Conn: conn,
	}
	return
}


func (c *Client) sendFrame(status int, data []byte) {
	req := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": "4CC5779A",
			"device_id":"123457",
			"device.imsi":"23344543",
		},
		"business": map[string]interface{}{
			"aue":   "raw",
			"tte":   "UTF8",
			//"vcn":   "x_catherine",
			"vcn":   "xiaoyan",
			"pitch": 50,
			"ent":   "aisound",
			"speed" :50,
			"sta":"3",
		//	"smk":"0",
		},
		"data": map[string]interface{}{
			"status":   status,
			"text":     base64.StdEncoding.EncodeToString(data),
			"encoding": "",
		},
	}
	c.WriteJSON(req)
}

func (c *Client) sendLast() {
	b, _ := ReadFileBytes(*file)
	c.sendFrame(2, b)
}

func (c *Client) Read() {
	f, err := os.Create(*file + ".pcm")
	defer f.Close()
	if err != nil {
		panic(err)
	}
	sta:=time.Now()
	for {
		var res Result
		_, resp, err := c.ReadMessage()

		if err != nil {
			fmt.Println(err)
			return
		}

		json.Unmarshal(resp, &res)
		if res.Code != 0 {
			fmt.Println(string(resp))
			fmt.Println("total cost",time.Since(sta))
			continue
		}
		fmt.Println("resp=>",string(resp))
		//if len(res.Data.Audio)==0{
		//	fmt.Println("data len == 0 ")
		//}
	//	fmt.Println(res.Data.Spell,res.Sid,res.Data.Status)
		if res.Code == 0 {
			if res.Data == nil {
				continue
			}
			//fmt.Println("sid:", res.Sid, " status:", res.Data.Status, " ced: ", res.Data.Ced, " spell: ", res.Data.Spell)
			audio,err:=base64.StdEncoding.DecodeString(res.Data.Audio)
			//spe,err:=base64.StdEncoding.DecodeString(res.Data.Spell)
			if err != nil {
				//panic(err)
			}else{
				//fmt.Println(ConvertByte2String(spe,"gb2312"))
				//fmt.Println("ced=>",res.Data.Ced)
			}

			f.Write(audio)
			// status == 2 表示音频数据返回结束。可以关闭连接
			if res.Data.Status == 2 {
				fmt.Println("合成结束，音频保存在："+f.Name())
				break
			}
		} else {
			fmt.Println(res.Code, res.Message)
		}

	}
}

type Result struct {
	Code    int    `json:"code"`
	Message string `json:"message"`
	Sid     string `json:"sid"`
	Data    *Data  `json:"data"`
}

type Data struct {
	Status int         `json:"status"`
	Audio  string      `json:"audio"`
	Ced    string      `json:"ced"`
	Spell  string`json:"spell"`
}
//
//func ConvertByte2String(byte []byte, charset string) string {
//
//	var str string
//	switch charset {
//	case "gb2312":
//		var decodeBytes,_=simplifiedchinese.GBK.NewDecoder().Bytes(byte)
//		str= string(decodeBytes)
//	case "utf-8":
//		fallthrough
//	default:
//		str = string(byte)
//	}
//
//	return str
//}

