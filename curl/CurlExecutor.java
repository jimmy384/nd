package jimmy.practice.basic.common.utils.curl;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

import java.io.IOException;
import java.util.Map.Entry;

public class CurlExecutor {
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();

    public static void main(String[] args) {
        String cmd = "curl 'https://api.bilibili.com/x/web-interface/wbi/search/all/v2?__refresh__=true&_extra=&context=&page=1&page_size=42&order=&duration=&from_source=&from_spmid=333.337&platform=pc&highlight=1&single_column=0&keyword=%E5%B7%A8%E4%BA%BAreaction&qv_id=birp2J39j1nlIrF8me4l0sbEIOL4ecON&ad_resource=5646&source_tag=3&web_location=1430654&w_rid=464462d80dd3c72e7a13b34eed1cf29c&wts=1724169728' \\\n" +
                "  -H 'accept: application/json, text/plain, */*' \\\n" +
                "  -H 'accept-language: zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6' \\\n" +
                "  -H $'cookie: buvid3=CAEE5405-7A01-98A3-CA7E-7B5322C96DEA90939infoc; b_nut=1723737090; _uuid=47510F385-5E62-8CAE-7106C-B45DCD37EAC491564infoc; enable_web_push=DISABLE; SESSDATA=0138dc8c%2C1739289147%2Cc233f%2A81CjCrRjadGt5AOsTWppeItsLllWf_XaX47pxztA9NXxCySUxctpKJWciix6FNK2dxO8wSVkhycmp0cnlybjBEU2oyVUR5VWpETXNSanJPZ0p0bXM2MGY3ZWVyUEh6WHNrRDQxUlhaTUowWjNwRHU2dXZaTlhuTUZDV3RkcFYzMWF4dkdkY2NmVk9nIIEC; bili_jct=1ca1fcf4e799e661a17ad91bd6f98afa; DedeUserID=1662503; DedeUserID__ckMd5=1039551e32988c71; sid=6ayfoxpr; header_theme_version=CLOSE; CURRENT_FNVAL=4048; rpdid=|(J~|~uJJYlJ0J\\'u~kJ~Jm~lk; buvid4=9FAAA2B8-6DBE-5921-2AE2-327BC89AD8D872573-022101502-VheKLZviuDG%2B0NO5TV6mZA%3D%3D; fingerprint=765725a36c9ed9d2ea54a14b30b9b436; buvid_fp_plain=undefined; CURRENT_QUALITY=80; home_feed_column=5; browser_resolution=1912-875; bili_ticket=eyJhbGciOiJIUzI1NiIsImtpZCI6InMwMyIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MjQyNTM2OTEsImlhdCI6MTcyMzk5NDQzMSwicGx0IjotMX0.orA6OEG_2l_Y6JUZjXg0Hnqyteet5cjKQ8XO7T154BI; bili_ticket_expires=1724253631; buvid_fp=765725a36c9ed9d2ea54a14b30b9b436; bp_t_offset_1662503=967779629278429184; b_lsid=3DC447E9_1917084C34E' \\\n" +
                "  -H 'origin: https://search.bilibili.com' \\\n" +
                "  -H 'priority: u=1, i' \\\n" +
                "  -H 'referer: https://search.bilibili.com/all?keyword=%E5%B7%A8%E4%BA%BAreaction&search_source=1' \\\n" +
                "  -H 'sec-ch-ua: \"Not)A;Brand\";v=\"99\", \"Microsoft Edge\";v=\"127\", \"Chromium\";v=\"127\"' \\\n" +
                "  -H 'sec-ch-ua-mobile: ?0' \\\n" +
                "  -H 'sec-ch-ua-platform: \"macOS\"' \\\n" +
                "  -H 'sec-fetch-dest: empty' \\\n" +
                "  -H 'sec-fetch-mode: cors' \\\n" +
                "  -H 'sec-fetch-site: same-site' \\\n" +
                "  -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 Edg/127.0.0.0'";
        execute(cmd);
    }

    public static void execute(String curlCmd) {
        RequestInfo requestInfo = CurlParser.parse(curlCmd);
        ClassicRequestBuilder requestBuilder = ClassicRequestBuilder.create(requestInfo.getMethod().name())
                .setUri(requestInfo.getUrl());
        for (Entry<String, String> entry : requestInfo.getHeaders().entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }
        for (Entry<String, String> entry : requestInfo.getParams().entrySet()) {
            requestBuilder.addParameter(entry.getKey(), entry.getValue());
        }
        if (StringUtils.isNotBlank(requestInfo.getBody())) {
            requestBuilder.setEntity(requestInfo.getBody(), ContentType.APPLICATION_JSON);
        }
        ClassicHttpRequest request = requestBuilder.build();

        try {
            String body = HTTP_CLIENT.execute(request, response -> {
                return EntityUtils.toString(response.getEntity());
            });
            System.out.println(body);
        } catch (IOException e) {
            throw new IllegalStateException("请求失败, path:" + request.getRequestUri(), e);
        }
    }
}
