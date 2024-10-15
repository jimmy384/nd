$(document).ready(() => {
    console.log('执行popup.js')

    chrome.runtime.sendMessage({ action: "getConfig" }, response => {
        console.log("获取配置:", response)
        $("#backendUrlInput").val(response.backendUrl)
    });

    $("#saveBtn").click(event => {
        const backendUrl = $("#backendUrlInput").val().trim()
        const message = {
            action: "saveConfig", settings: { "backendUrl": backendUrl }
        }
        chrome.runtime.sendMessage(message, response => {
            console.log("保存设置结果:", response)
        });
    })
})
