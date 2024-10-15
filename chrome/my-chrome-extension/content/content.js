$(document).ready(() => {
  // 使用jQuery找到页面的第一个输入框
  var firstInput = $('input:first')
  if (firstInput.length) {
    firstInput.css('border', '2px solid red'); // 给它添加一个红色边框以示识别
    console.log("找到的第一个输入框: ", firstInput)

    const message = { action: "forwardRequest", path: "/test", method: "POST", param: { "name": "jimmy" } }
    chrome.runtime.sendMessage(message, response => {
      console.log("保存设置结果:", response)
    })
  } else {
    console.log("没有找到输入框")
  }
});