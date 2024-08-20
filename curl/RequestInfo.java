package jimmy.practice.basic.common.utils.curl;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Map;

@Data
@Builder
public class RequestInfo {
    private final HttpMethod method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String body;
}
