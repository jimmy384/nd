package jimmy.practice.spd.component.user.curl.parser;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Http请求的关键信息
 *
 * @author jimmy
 */
@Data
@Builder
public class RequestInfo {
    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String body;

    public ContentType getContentType() {
        ContentType contentType = null;
        if (MapUtils.isNotEmpty(headers)) {
            for (Entry<String, String> entry : headers.entrySet()) {
                String headerName = entry.getKey();
                if (StringUtils.equalsIgnoreCase(headerName, HttpHeaders.CONTENT_TYPE)) {
                    String headerValue = entry.getValue();
                    if (StringUtils.containsAnyIgnoreCase(headerValue, ContentType.APPLICATION_JSON.getMimeType())) {
                        contentType = ContentType.APPLICATION_JSON;
                    } else if (StringUtils.containsAnyIgnoreCase(headerValue, ContentType.MULTIPART_FORM_DATA.getMimeType())) {
                        contentType = ContentType.MULTIPART_FORM_DATA;
                    } else if (StringUtils.containsAnyIgnoreCase(headerValue, ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                        contentType = ContentType.APPLICATION_FORM_URLENCODED;
                    }
                }
            }
        }
        return contentType;
    }
}
