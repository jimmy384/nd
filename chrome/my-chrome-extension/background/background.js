// 读取配置参数
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "getConfig") {
        chrome.storage.local.get(null, result => {
            sendResponse(result);
        });
    }
    return true;
});

// 保存配置参数
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "saveConfig") {
        chrome.storage.local.set(message.settings, () => {
            console.log("配置参数已保存");
            sendResponse({ "status": "success" });
        });
    }
    return true;
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.action === "forwardRequest") {
        chrome.storage.local.get(null, result => {
            const url = result.backendUrl + message.path
            fetch(url, {
                method: message.method,
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(message.param),
            }).then(response => {
                if (response.status !== 200) {
                    throw new Error(`请求失败, 响应码:${response.status}`)
                }
                return response.json()
            }).then(data => {
                console.log("处理接口返回数据", data);
                sendResponse(data);
            }).catch(error => {
                console.error('请求出错:', error);
            });
        });
    }
    return true;
})