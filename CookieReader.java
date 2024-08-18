package jimmy.practice.basic.common.utils.cookie;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import jimmy.practice.basic.common.utils.cookie.vo.Cookie;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class CookieReader {
    private final String cookieFilePath;

    public static CookieReader createByBrowserDataDumpPath(String browserDataDumpPath) {
        String path = FilenameUtils.concat(browserDataDumpPath, "microsoft_edge_default_cookie.json");
        return new CookieReader(path);
    }

    public CookieReader(String cookieFilePath) {
        this.cookieFilePath = cookieFilePath;
    }

    public String getCookie(String url) {
        List<Cookie> cookieList;
        try (FileInputStream fis = new FileInputStream(cookieFilePath)) {
            String json = IOUtils.toString(fis, StandardCharsets.UTF_8);
            cookieList = JSON.parseArray(json, Cookie.class);
        } catch (IOException e) {
            throw new IllegalStateException("读取Cookie数据失败, cookieFilePath:" + cookieFilePath, e);
        }

        String host = URLUtil.getHost(URLUtil.url(url)).getHost();
        String path = URLUtil.getPath(url);
        return cookieList.stream()
                .filter(cookie -> host.contains(cookie.getHost()) && path.startsWith(cookie.getPath()))
                .map(Cookie::getKeyValue)
                .collect(Collectors.joining(";"));
    }
}
