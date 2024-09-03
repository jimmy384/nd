package jimmy.practice.spd.component.user.cookie;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson2.JSON;
import jimmy.practice.basic.common.utils.cookie.vo.Cookie;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cookie获取器
 *
 * @author jimmy
 */
@RequiredArgsConstructor
public class CookieReader {
    private final HackBrowserData hackBrowserData;
    private List<Cookie> cookieList;

    /**
     * 根据Url获取对应的Cookie
     *
     * @param url 请求Url
     * @return Cookie
     */
    public String getCookie(String url) {
        if (cookieList == null) {
            reloadCookie();
        }

        String host = URLUtil.getHost(URLUtil.url(url)).getHost();
        String path = URLUtil.getPath(url);
        return cookieList.stream()
                .filter(cookie -> host.contains(cookie.getHost()) && path.startsWith(cookie.getPath()))
                .map(Cookie::getKeyValue)
                .collect(Collectors.joining(";"));
    }

    public void reloadCookie() {
        hackBrowserData.dump();
        String cookieFilePath = FilenameUtils.concat(hackBrowserData.getResultPath(), "microsoft_%s_default_cookie.json".formatted(hackBrowserData.getBrowserType()));
        try (FileInputStream fis = new FileInputStream(cookieFilePath)) {
            String json = IOUtils.toString(fis, StandardCharsets.UTF_8);
            cookieList = JSON.parseArray(json, Cookie.class);
        } catch (IOException e) {
            throw new IllegalStateException("读取Cookie数据失败, cookieFilePath:" + cookieFilePath, e);
        }
    }
}
