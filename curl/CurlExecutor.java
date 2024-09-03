package jimmy.practice.spd.component.user.curl;

import jimmy.practice.basic.common.utils.cookie.CookieReader;
import jimmy.practice.basic.common.utils.curl.parser.CurlParser;
import jimmy.practice.basic.common.utils.curl.parser.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 执行curl命令(使用Java模拟curl命令的含义发送Http请求)
 *
 * @author jimmy
 */
@RequiredArgsConstructor
public class CurlExecutor {
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    private final CookieReader cookieReader;

    public String execute(String curlCmd) {
        return doExecute(curlCmd, response -> {
            return EntityUtils.toString(response.getEntity());
        });
    }

    public <T> T execute(String curlCmd, HttpClientResponseHandler<T> responseHandler) {
        return doExecute(curlCmd, responseHandler);
    }

    private <T> T doExecute(String curlCmd, HttpClientResponseHandler<T> stringHttpClientResponseHandler) {
        RequestInfo requestInfo = CurlParser.parse(curlCmd);
        ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(requestInfo.getMethod().name())
                .setUri(requestInfo.getUrl());

        Map<String, String> params = requestInfo.getParams();
        if (MapUtils.isNotEmpty(params)) {
            for (Entry<String, String> entry : params.entrySet()) {
                requestBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        Map<String, String> headers = requestInfo.getHeaders();
        String cookie = cookieReader.getCookie(requestInfo.getUrl());
        if (StringUtils.isNotBlank(cookie)) {
            headers.put(HttpHeaders.COOKIE, cookie);
        }
        if (MapUtils.isNotEmpty(headers)) {
            for (Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }


        if (StringUtils.isNotBlank(requestInfo.getBody())) {
            requestBuilder.setEntity(requestInfo.getBody(), requestInfo.getContentType());
        }
        ClassicHttpRequest request = requestBuilder.build();

        try {
            return HTTP_CLIENT.execute(request, stringHttpClientResponseHandler);
        } catch (IOException e) {
            throw new IllegalStateException("请求失败, path:" + request.getRequestUri(), e);
        }
    }
}
