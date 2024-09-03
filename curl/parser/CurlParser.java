package jimmy.practice.spd.component.user.curl.parser;

import cn.hutool.core.net.url.UrlBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 解析curl命令，提取出Http请求的关键信息
 *
 * @author jimmy
 */
public class CurlParser {
    private static final char SINGLE_QUOTE = '\'';
    private static final char BACKSLASH = '\\';
    private static final char LINE_BREAK = '\n';
    private static final char SPACE = ' ';
    private static final char SPLITTER = '\0';
    private static final char DOLLAR = '$';
    private static CommandLineParser parser;
    private static Options options;

    public static RequestInfo parse(String curlCmd) {
        String[] curlArgs = curlCmdToArgs(curlCmd);
        CommandLine commandLine = parseCurlArgs(curlArgs);

        String url = commandLine.getArgs()[0];
        Map<String, String> queryParamMap = getQueryParamMap(url);
        Map<String, String> headerMap = getHeaderMap(commandLine);
        String method = commandLine.getOptionValue("X");
        String body = commandLine.getOptionValue("d");
        HttpMethod httpMethod;
        if (StringUtils.isBlank(method)) {
            if (StringUtils.isNotBlank(body)) {
                httpMethod = HttpMethod.POST;
            } else {
                httpMethod = HttpMethod.GET;
            }
        } else {
            httpMethod = HttpMethod.valueOf(method.toUpperCase());
        }
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


    private static CommandLine parseCurlArgs(String[] curlArgs) {
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
        try {
            return parser.parse(options, curlArgs);
        } catch (ParseException e) {
            throw new IllegalStateException("解析curl命令失败", e);
        }
    }


    private static String[] curlCmdToArgs(String curlCmd) {
        char[] charArray = curlCmd.toCharArray();
        boolean leftSingleQuote = false;

        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == SPACE && !leftSingleQuote) {
                charArray[i] = SPLITTER;
            }
            if (c == SINGLE_QUOTE) {
                if (leftSingleQuote && charArray[i - 1] != BACKSLASH) {
                    leftSingleQuote = false;
                } else {
                    leftSingleQuote = true;
                }
            }
            if ((c == LINE_BREAK || c == BACKSLASH) && !leftSingleQuote) {
                charArray[i] = SPLITTER;
            }
        }
        String[] args = new String(charArray).split(String.valueOf(SPLITTER));
        args = Arrays.copyOfRange(args, 1, args.length);
        return Arrays.stream(args).filter(str -> !str.trim().isEmpty())
                .map(str -> {
                    if (str.charAt(0) == SINGLE_QUOTE && str.charAt(str.length() - 1) == SINGLE_QUOTE) {
                        return str.substring(1, str.length() - 1);
                    } else if (str.charAt(0) == DOLLAR && str.charAt(1) == SINGLE_QUOTE && str.charAt(str.length() - 1) == SINGLE_QUOTE) {
                        return str.substring(2, str.length() - 1);
                    } else {
                        return str;
                    }
                })
                .toArray(String[]::new);
    }
}
