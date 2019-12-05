using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Security.Cryptography;
using System.Web;

namespace webgate_ws_csharp.Common
{
    public class Authentication
    {
        const string HTTP_SEPARATE = "?";
        const string HTTP_VERSION = "HTTP/1.1";
        const string WHITE_SPACE = " ";
        const string HTTP_HEADER_HOST = "host: ";
        const string HTTP_HEADER_DATE = "date: ";

        private string _apiKey;
        private string _apiSecret;
        private string authTemplate = "api_key=\"{0}\",algorithm=\"hmac-sha256\",headers=\"host date request-line\",signature=\"{1}\"";

        public enum HttpMethod
        {
            GET,
            POST
        }

        public Authentication(string apiKey, string apiSecret)
        {
            if (string.IsNullOrEmpty(apiKey) || string.IsNullOrEmpty(apiSecret))
            {
                throw new Exception("apiKey and apiSecret must not be null or empty.");
            }
            else
            {
                _apiKey = apiKey;
                _apiSecret = apiSecret;
            }
        }

        public string GenerateAuthUrl(string url, HttpMethod method)
        {
            Uri u = new Uri(url);
            StringBuilder strBuilder = new StringBuilder();
            string utcDate = String.Format("{0:R}", DateTime.UtcNow);

            string host = HTTP_HEADER_HOST + u.Host;
            string date = HTTP_HEADER_DATE + utcDate;
            string requestLine = method.ToString() + WHITE_SPACE + u.AbsolutePath + WHITE_SPACE + HTTP_VERSION;

            strBuilder.AppendFormat("{0}\n", host);
            strBuilder.AppendFormat("{0}\n", date);
            strBuilder.AppendFormat("{0}", requestLine);

            string signature = EncodeByHMACSHA256(_apiSecret, strBuilder.ToString());
            string authorization = Convert.ToBase64String(Encoding.UTF8.GetBytes(string.Format(authTemplate, _apiKey, signature)));

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.AppendFormat("host={0}", UrlEncode(u.Host, Encoding.UTF8));
            queryBuilder.AppendFormat("&date={0}", UrlEncode(utcDate, Encoding.UTF8));
            queryBuilder.AppendFormat("&authorization={0}", UrlEncode(authorization, Encoding.UTF8));

            return url + HTTP_SEPARATE + queryBuilder.ToString();
        }


        private static string UrlEncode(string temp, Encoding encoding)
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < temp.Length; i++)
            {
                string t = temp[i].ToString();
                string k = HttpUtility.UrlEncode(t, encoding);
                if (t == k)
                {
                    stringBuilder.Append(t);
                }
                else
                {
                    stringBuilder.Append(k.ToUpper());
                }
            }
            return stringBuilder.ToString();
        }

        public string EncodeByHMACSHA256(string apiSecret, string input)
        {
            Encoding encoding = Encoding.UTF8;
            using (HMACSHA256 hmac = new HMACSHA256(encoding.GetBytes(apiSecret)))
            {
                byte[] encodedData = hmac.ComputeHash(encoding.GetBytes(input));

                return Convert.ToBase64String(encodedData);
            }
        }
    }
}
