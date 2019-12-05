using System;
using System.Collections.Generic;
using System.Text;
using WebSocketSharp;
using webgate_ws_csharp.Common;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System.IO;
using System.Threading;

namespace webgate_ws_csharp
{
    class StartIAT
    {
        const int SUCCESS = 0;
        const int FRAME_SIZE = 1220; //每一帧的音频大小
        const int INTERVAL = 40; //发送音频间隔(ms)
        StreamStatus STREAM_STATUS = StreamStatus.STATUS_FIRST_FRAME; //音频的状态信息，标识音频是第一帧，还是中间帧、最后一帧
        delegate void SendDataDelegate(WebSocket ws, string appId, string audioPath);


        public void Call(string authUrl, string appId, string audioPath)
        {
            using (var ws = new WebSocket(authUrl))
            {
                ws.OnOpen += (sender, e) =>
                {
                    // Send data
                 
                    Action<WebSocket, string, string> s = (a, b, c) =>
                    {
                        this.SendData(a, b, c);
                    };

                    s.BeginInvoke(ws, appId, audioPath, null, null);
                };

                ws.OnMessage += (sender, e) =>
                {
                    // Receive and process data while sending data
                    this.ProcessResult(ws, e.Data);
                };

                ws.OnError += (sender, e) =>
                {
                    Console.WriteLine(e.Message);
                };

                ws.OnClose += (sender, e) =>
                {
                    Console.WriteLine(e.Code);
                };

                ws.Connect();
                Console.ReadKey(true);
            }
        }

        /// <summary>
        /// Send data which read from audio file
        /// </summary>
        /// <param name="ws">websocket handle</param>
        /// <param name="appId">app_id</param>
        /// <param name="audioPath">audio file path</param>
        private void SendData(WebSocket ws, string appId, string audioPath)
        {
            BinaryReader br = new BinaryReader(new FileStream(audioPath, FileMode.Open));
            byte[] currentFrame = br.ReadBytes(FRAME_SIZE);
            bool moreData = true;

            // send first frame
            ws.Send(GenerateData(appId, STREAM_STATUS, currentFrame));
            while (moreData)
            {
                currentFrame = br.ReadBytes(FRAME_SIZE);
                if (currentFrame.Length != 0)
                {
                    STREAM_STATUS = StreamStatus.STATUS_CONTINUE_FRAME;
                }
                else
                {
                    STREAM_STATUS = StreamStatus.STATUS_LAST_FRAME;
                    moreData = false;
                }

                ws.Send(GenerateData(appId, STREAM_STATUS, currentFrame));
            }

            br.Close();
        }

        /// <summary>
        /// Process Json result
        /// </summary>
        /// <param name="ws">websocket handle</param>
        /// <param name="data">response data from server</param>
        private void ProcessResult(WebSocket ws, string data)
        {
            if (!String.IsNullOrEmpty(data))
            {
                JObject obj = JObject.Parse(data);
                var code = obj.GetValue("code");

                if (int.Parse(code.ToString()) == SUCCESS)
                {
                    var d = obj.GetValue("data");
                    if (d != null)
                    {
                        int status = d.Value<int>("status");
                        JObject result = d.Value<JObject>("result");
                        if (status == (int)ResultStatus.STATUS_FINISH)
                        {
                            if (result != null)
                            {
                                Console.WriteLine(result.ToString());
                            }
                            ws.Close();
                        }
                        else
                        {
                            if (result != null)
                            {
                                Console.WriteLine(result.ToString());
                            }
                        }
                    }
                }
                else
                {
                    ws.Close();
                }

            }
        }

        /// <summary>
        /// Make data for request.
        /// </summary>
        /// <param name="appId">app_id</param>
        /// <param name="status">current status of stream</param>
        /// <param name="audio">audio content which converted to base64</param>
        /// <returns></returns>
        private string GenerateData(string appId, StreamStatus status, byte[] audio)
        {
            Thread.Sleep(INTERVAL);

            JObject commonObj = new JObject { 
            { "app_id", appId } 
            };
            JObject businessObj = new JObject {
            { "language", "zh_cn" }, 
            { "dwa", "wpgs" } 
            };
            JObject dataObj = new JObject { 
            { "status", (int)status }, 
            { "format", "audio/L16;rate=16000" },
            { "encoding", "raw" }, 
            { "audio", Convert.ToBase64String(audio) } 
            };
            JObject rootObj = new JObject { { "common", commonObj }, { "business", businessObj }, { "data", dataObj } };

            return rootObj.ToString();
        }
    }
}
