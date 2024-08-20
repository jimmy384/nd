package jimmy.practice.basic.common.utils.curl;

import cn.hutool.core.net.url.UrlBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CurlParser {
    private static CommandLineParser parser;
    private static Options options;

    public static void main(String[] args) throws Exception {
        String cmd = "curl 'https://api.vc.bilibili.com/session_svr/v1/session_svr/single_unread?build=0&mobi_app=web&unread_type=0' \\\n" +
                "  -H 'accept: application/json, text/plain, */*' \\\n" +
                "  -H 'accept-language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \\\n" +
                "  -H $'cookie: buvid3=CAEE5405-7A01-98A3-CA7E-7B5322C96DEA90939infoc; b_nut=1723737090; _uuid=47510F385-5E62-8CAE-7106C-B45DCD37EAC491564infoc; enable_web_push=DISABLE; SESSDATA=0138dc8c%2C1739289147%2Cc233f%2A81CjCrRjadGt5AOsTWppeItsLllWf_XaX47pxztA9NXxCySUxctpKJWciix6FNK2dxO8wSVkhycmp0cnlybjBEU2oyVUR5VWpETXNSanJPZ0p0bXM2MGY3ZWVyUEh6WHNrRDQxUlhaTUowWjNwRHU2dXZaTlhuTUZDV3RkcFYzMWF4dkdkY2NmVk9nIIEC; bili_jct=1ca1fcf4e799e661a17ad91bd6f98afa; DedeUserID=1662503; DedeUserID__ckMd5=1039551e32988c71; sid=6ayfoxpr; header_theme_version=CLOSE; CURRENT_FNVAL=4048; rpdid=|(J~|~uJJYlJ0J\\'u~kJ~Jm~lk; buvid4=9FAAA2B8-6DBE-5921-2AE2-327BC89AD8D872573-022101502-VheKLZviuDG%2B0NO5TV6mZA%3D%3D; fingerprint=765725a36c9ed9d2ea54a14b30b9b436; buvid_fp_plain=undefined; CURRENT_QUALITY=80; home_feed_column=5; browser_resolution=1912-875; bili_ticket=eyJhbGciOiJIUzI1NiIsImtpZCI6InMwMyIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MjQyNTM2OTEsImlhdCI6MTcyMzk5NDQzMSwicGx0IjotMX0.orA6OEG_2l_Y6JUZjXg0Hnqyteet5cjKQ8XO7T154BI; bili_ticket_expires=1724253631; b_lsid=CD8E41DA_1916F5D66B3; buvid_fp=765725a36c9ed9d2ea54a14b30b9b436; bp_t_offset_1662503=967722660832215040' \\\n" +
                "  -H 'origin: https://search.bilibili.com' \\\n" +
                "  -H 'priority: u=1, i' \\\n" +
                "  -H 'referer: https://search.bilibili.com/all?vt=54056063&keyword=%E6%99%BA%E8%83%BD%E8%B7%AF%E9%9A%9C&search_source=1' \\\n" +
                "  -H 'sec-ch-ua: \"Not)A;Brand\";v=\"99\", \"Microsoft Edge\";v=\"127\", \"Chromium\";v=\"127\"' \\\n" +
                "  -H 'sec-ch-ua-mobile: ?0' \\\n" +
                "  -H 'sec-ch-ua-platform: \"macOS\"' \\\n" +
                "  -H 'sec-fetch-dest: empty' \\\n" +
                "  -H 'sec-fetch-mode: cors' \\\n" +
                "  -H 'sec-fetch-site: same-site' \\\n" +
                "  -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0' \\\n" +
                " --data $'{\"name\": \"jimmy\"}'";
        RequestInfo requestInfo = parse(cmd);
        System.out.println(requestInfo);
    }

    public static RequestInfo parse(String curlCmd) throws ParseException {
        String[] curlArgs = curlCmdToArgs(curlCmd);
        CommandLine commandLine = parseCurlArgs(curlArgs);

        String url = commandLine.getArgs()[0];
        Map<String, String> queryParamMap = getQueryParamMap(url);
        String method = commandLine.getOptionValue("X");
        HttpMethod httpMethod = StringUtils.isBlank(method) ? HttpMethod.POST : HttpMethod.valueOf(method.toUpperCase());
        Map<String, String> headerMap = getHeaderMap(commandLine);
        String body = commandLine.getOptionValue("d");
        return RequestInfo.builder()
                .method(httpMethod)
                .url(url)
                .headers(headerMap)
                .params(queryParamMap)
                .body(body)
                .build();
    }

    private static Map<String, String> getQueryParamMap(String url) {
        UrlBuilder urlBuilder = UrlBuilder.ofHttp(url, StandardCharsets.UTF_8);
        Map<CharSequence, CharSequence> queryMap = urlBuilder.getQuery().getQueryMap();
        Map<String, String> paramMap = new HashMap<>(queryMap.size());
        for (Entry<CharSequence, CharSequence> entry : queryMap.entrySet()) {
            paramMap.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return paramMap;
    }

    private static Map<String, String> getHeaderMap(CommandLine commandLine) {
        String[] headers = commandLine.getOptionValues("H");
        Map<String, String> headerMap = new HashMap<>(headers.length);
        for (String header : headers) {
            int index = header.indexOf(":");
            String headerName = header.substring(0, index);
            String headerValue = header.substring(index + 1).trim();
            headerMap.put(headerName, headerValue);
        }
        return headerMap;
    }


    private static CommandLine parseCurlArgs(String[] curlArgs) throws ParseException {
        if (parser == null || options == null) {
            DefaultParser newParser = new DefaultParser();
            Options newOptions = new Options();
            newOptions.addOption("X", "request", true, "请求方法");
            newOptions.addOption("H", "header", true, "请求头");
            newOptions.addOption("d", "data-raw", true, "请求头");
            newOptions.addOption("d", "data", true, "请求头");
            parser = newParser;
            options = newOptions;
        }
        return parser.parse(options, curlArgs);
    }


    private static String[] curlCmdToArgs(String curlCmd) {
        char[] charArray = curlCmd.toCharArray();
        boolean leftSingleQuote = false;
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == ' ' && !leftSingleQuote) {
                charArray[i] = '\0';
            }
            if (c == '\'') {
                if (leftSingleQuote && charArray[i - 1] != '\\') {
                    leftSingleQuote = false;
                } else {
                    leftSingleQuote = true;
                }
            }
            if ((c == '\n' || c == '\\') && !leftSingleQuote) {
                charArray[i] = ' ';
            }
        }
        String[] args = new String(charArray).split("\0");
        args = Arrays.copyOfRange(args, 1, args.length);
        return Arrays.stream(args).filter(str -> !str.trim().isEmpty())
                .map(str -> {
                    if (str.charAt(0) == '\'' && str.charAt(str.length() - 1) == '\'') {
                        return str.substring(1, str.length() - 1);
                    } else if (str.charAt(0) == '$' && str.charAt(1) == '\'' && str.charAt(str.length() - 1) == '\'') {
                        return str.substring(2, str.length() - 1);
                    } else {
                        return str;
                    }
                })
                .toArray(String[]::new);
    }
}
