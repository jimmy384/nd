// ==UserScript==
// @name         收集网站Cookie
// @description  收集网站的Cookie，发送到后台
// @version      1.0
// @namespace    https://www.tampermonkey.net/
// @author       Jimmy
// @match        *://*.baidu.com/*
// @require      https://cdn.bootcss.com/jquery/3.7.1/jquery.min.js
// @require      https://scriptcat.org/lib/637/1.4.3/ajaxHooker.js#sha256=y1sWy1M/U5JP1tlAY5e80monDp27fF+GMRLsOiIrSUY=
// @run-at       document-end
// @include      https://api.bilibili.com
// @connect      *
// @grant        unsafeWindow
// @grant        GM_cookie
// @grant        GM_xmlhttpRequest
// @grant        GM_addStyle


// ==/UserScript==

(function() {
    'use strict';

    addScriptAndCss()
    collectCookie();
    ajaxRequest();

    function getElementSelectors(element) {
        let result = []
        let selector = '';
        let cnt = 0;
        for (; element && element.nodeType == Node.ELEMENT_NODE; element = element.parentNode) {
            let eleClass = getClass(element)
            if (eleClass) {
                cnt++;
                selector = eleClass + selector
                result.push(selector)
                if (cnt > 2) {
                    break
                }
            } else {
                selector = " " + element.nodeName.toLowerCase() + selector
            }
        }
        return result;
    }

    function getClass(element) {
        var result = "";
        for (var i = 0; i < element.classList.length; i++) {
            if (element.classList[i] != "gm_highlight") {
                result = result + "." + element.classList[i]
            }
        }
        return result
    }

    function addScriptAndCss() {
        var script = document.createElement('script');
        script.src = 'https://cdn.jsdelivr.net/npm/vue@3';
        document.head.appendChild(script);

        const cssText = `
            .gm_highlight {outline: 1px solid #000;}
            #myApp {
              background: #eee;
              z-index: 9999;
              position: fixed;
              left:0;top: 0;
              width: 200px;
              height:100px;


            .no-select {
              user-select: none;
            }
            `
        GM_addStyle(cssText);
    }

    function collectCookie() {
        GM_cookie.list({}, function(cookies, error) {
            if (!error) {
                const saveCookieParam = {
                    cookies: cookies
                }
                console.log(saveCookieParam)
            } else {
                console.error(error);
            }
        });
    }

    function ajaxRequest() {
        /*
        type 可选，应是xhr或fetch
        url 可选，字符串或正则表达式，无需完全匹配
        method 可选，不区分大小写
        async 可选，布尔值
        */
        ajaxHooker.filter([
            {type: 'xhr', url: 'https://api.bilibili.com/x/web-interface/wbi/search/all/v2', method: 'GET', async: true}
        ])

        ajaxHooker.hook(request => {
            console.log("拦截请求", request)
            request.response = res => {
                console.log("拦截响应", res);
                // 响应数据是哪个属性取决于哪个属性被读取，xhr可能的属性为response、responseText、responseXML，fetch可能的属性为arrayBuffer、blob、formData、json、text
            };
        })

        // 请求其他网站的例子1，测试了是会自动带上该网站的cookie的
        /*
        const url = "https://api.bilibili.com/x/v3/fav/folder/created/list?pn=1&ps=10&up_mid=1662503";
        GM_xmlhttpRequest({
            method: "GET",
            url: url,
            headers: {
                "Content-Type": "application/json"
            },
            onload: function(response) {
                console.log("请求B站接口")
                console.log(response.responseText)
            }
        });
        */

        // 请求其他网站的例子2，测试了是会自动带上该网站的cookie的
        /*
        const favUrl = "https://api.bilibili.com/x/v3/fav/resource/deal";
        GM_xmlhttpRequest({
            method: "POST",
            url: favUrl,
            data: "rid=1606707323&type=2&add_media_ids=47314803&del_media_ids=&platform=web&eab_x=2&ramval=1&ga=1&gaia_source=web_normal&csrf=1ca1fcf4e799e661a17ad91bd6f98afa",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            onload: function(response) {
                console.log("请求B站接口")
                console.log(response.responseText)
            }
        });
        */
    }

    const text = `<div id="myApp" class="no-select">
           {{ message }}
           <button @click="startSelect">开启选定元素(Alt+Q停止)</button>
           <button @click="stopSelect">停止选定元素(Alt+Q停止)</button>
           <my-component></my-component>
    </div>`

    var el=document.createElement('div')
    el.innerHTML = text;
    document.body.append(el)

    // 定义一个组件
    const MyComponent = {
        template: '<button @click="handleClick">Click me!</button>',
        methods: {
            handleClick() {
                console.log('Button clicked!')
            }
        }
    }

    const mouseoverEventHandler = function(event) {
        const target = event.target;
        target.classList.add('gm_highlight');
        const selectors = getElementSelectors(target)
        console.log(selectors)
    };
    const mouseoutEventHandler = function(event) {
        const target = event.target;
        target.classList.remove('gm_highlight');
    }

    const App = {
        components: {
            MyComponent
        },
        data() {
            return {
                message: "Hello Element Plus",
            };
        },
        methods: {
            startSelect() {
                document.addEventListener('mouseover', mouseoverEventHandler);
                document.addEventListener('mouseout', mouseoutEventHandler);
            },
            stopSelect() {
                document.removeEventListener('mouseover', mouseoverEventHandler);
                document.removeEventListener('mouseout', mouseoutEventHandler);
            }
        }
    };

    unsafeWindow.addEventListener('load', () => {
        var Vue = unsafeWindow.Vue;
        const app = Vue.createApp(App);
        // app.use(ElementPlus);
        app.mount("#myApp");


        var draggable = document.getElementById("myApp");
        var isMouseDown,initX,initY,height = draggable.offsetHeight,width = draggable.offsetWidth;

        draggable.addEventListener('mousedown', function(e) {
            isMouseDown = true;
            document.body.classList.add('no-select');
            initX = e.offsetX;
            initY = e.offsetY;
        })

        document.addEventListener('mousemove', function(e) {
            if (isMouseDown) {
                var cx = e.clientX - initX,
                    cy = e.clientY - initY;
                if (cx < 0) {
                    cx = 0;
                }
                if (cy < 0) {
                    cy = 0;
                }
                if (window.innerWidth - e.clientX + initX < width) {
                    cx = window.innerWidth - width;
                }
                if (e.clientY > window.innerHeight - height+ initY) {
                    cy = window.innerHeight - height;
                }
                draggable.style.left = cx + 'px';
                draggable.style.top = cy + 'px';
            }
        })

        draggable.addEventListener('mouseup', function() {
            isMouseDown = false;
            document.body.classList.remove('no-select');
        })

        document.addEventListener('mouseup', function(e) {
            if(e.clientY > window.innerHeight || e.clientY < 0 || e.clientX < 0 ||e.clientX > window.innerWidth){
                isMouseDown = false;
                document.body.classList.remove('no-select');
            }
        });
    });

})();