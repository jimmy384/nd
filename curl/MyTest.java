package jimmy.practice.basic.common.utils.curl;

import jimmy.practice.basic.common.utils.cookie.CookieReader;
import jimmy.practice.basic.common.utils.cookie.HackBrowserData;

public class MyTest {
    public static void main(String[] args) {
        HackBrowserData hackBrowserData = new HackBrowserData("edge");
        hackBrowserData.dump();

        CookieReader cookieReader = CookieReader.createByBrowserDataDumpPath(hackBrowserData.getResultPath());
        String url = "https://www.bilibili.com/s/search";
        String cookie = cookieReader.getCookie(url);

        System.out.println(cookie);
    }
}
