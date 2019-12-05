using System;
using System.Collections.Generic;
using System.Text;
using WebSocketSharp;
using webgate_ws_csharp.Common;

namespace webgate_ws_csharp
{
    class Program
    {
        const string API_KEY = "***";
        const string API_SECRET = "***";
        const string APP_ID = "***";

        static void Main(string[] args)
        {
            CommandOption co = new CommandOption(args);

            CheckParams(co, "-s");

            Authentication.HttpMethod method = Authentication.HttpMethod.GET;
            Authentication authObj = new Authentication(API_KEY, API_SECRET);
            string authUrl = string.Empty;

            switch (co.Options["-s"])
            {
                case "iat":
                    // 听写
                    CheckParams(co, "-f");
                    StartIAT iat = new StartIAT();

                    authUrl = authObj.GenerateAuthUrl("ws://iat-api.xfyun.cn/v2/iat", method);
                    iat.Call(authUrl, APP_ID, co.Options["-f"]);
                    break;
                default:
                    break;
            }
        }

        static void CheckParams(CommandOption co, string param)
        {
            if (!co.Options.ContainsKey(param))
            {
                Console.WriteLine("Miss param \"" + param + "\".");
                Console.Read();
                return;
            }
        }
    }

    /// <summary>
    /// Designed for processing input params.
    /// </summary>
    class CommandOption
    {
        private Dictionary<string, string> options;

        public Dictionary<string, string> Options
        {
            get { return options; }
            set { options = value; }
        }

        public CommandOption(string[] args)
        {
            options = new Dictionary<string, string>();
            if (args.Length > 1)
            {
                int len = args.Length;
                if (len % 2 != 0)
                {
                    len -= 1;
                }

                for (int i = 0; i < len; i += 2)
                {
                    options.Add(args[i], args[i + 1]);
                }
            }
        }
    }
}
