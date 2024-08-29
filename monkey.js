// ==UserScript==
// @name         个人油猴插件
// @namespace    Jimmy
// @version      1.0
// @description  个人油猴插件
// @author       Jimmy
// @connect      www.bilibili.com
// @match        *://*.bilibili.com/*
// @match        *://*.baidu.com/*
// @require      https://cdn.bootcss.com/jquery/3.4.1/jquery.min.js
// @require      https://scriptcat.org/lib/637/1.4.3/ajaxHooker.js#sha256=y1sWy1M/U5JP1tlAY5e80monDp27fF+GMRLsOiIrSUY=
// @require      https://scriptcat.org/lib/721/1.0.1/gmCookie.js
// @run-at       document-start
// @grant        unsafeWindow
// @grant        GM_cookie
// ==/UserScript==

(function() {
    'use strict';

    ajaxHooker.filter([
        {type: 'xhr', url: 'www.baidu.com', method: 'GET', async: true},
        {url: /^http/},
    ])

    ajaxHooker.hook(request => {
        console.log(request.url)
        request.response = res => {
            console.log(res);
        };
    })

    let result = gmCookie('https://www.baidu.com/').then(async cookie => {
        // 读取cookie
        console.log(cookie);
        // 修改cookie
        //cookie.BAIDUID.value = 'hello';
        // 新增cookie
        //cookie.test = {
        //    path: '/',
        //    value: 'world'
        //};
        // 删除cookie
        //delete cookie.BAIDUID;
        // 由于GM_cookie是异步，调用$alldone方法可以等待所有操作完成
        await cookie.$alldone();
    })
    console.log("result")
    console.log(result)

    /*
    const originalOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (_, url) {
        if (isTartetUrl(url)) {
            this.addEventListener("readystatechange", function () {
                if (this.readyState === 4) {
                    const contentType = this.getResponseHeader("Content-Type");
                    if (contentType.indexOf("json") > -1) {
                        const res = JSON.parse(this.responseText);
                        res.data.key = "篡改";
                        this.responseText = res;
                        this.responseText = JSON.stringify(res);

                        Object.defineProperty(this, "response", {
                            writable: true,
                        });
                        Object.defineProperty(this, "responseText", {
                            writable: true,
                        });
                    }
                }
            });
        }
        originalOpen.apply(this, arguments);
    };

    function isTartetUrl(url) {
        return true;
    }
    */

})();
