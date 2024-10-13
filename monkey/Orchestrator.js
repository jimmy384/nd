class Orchestrator {
    constructor(definition) {
        this.definition = definition
        this.validate()
        this.domParser = new DOMParser()
    }
    /**
     * 校验流程定义对象的正确性
     */
    validate() {
        if (this.definition === null) {
            throw "流程定义definition不能为null"
        }
        const steps = Object.keys(this.definition)
        if (steps.length == 0) {
            throw "流程定义definition至少要定义1个step"
        }
        for (const step of steps) {
            const stepDefinition = this.definition[step]
            if (!stepDefinition.type) {
                throw `step缺少type字段, step:${step}`
            }
            if (stepDefinition.type == "fetchHtml") {
                if (!stepDefinition.extract) {
                    throw `step缺少extract字段, step:${step}`
                }
                for (const extract of stepDefinition.extract) {
                    if (extract.expr && !extract.obj) {
                        throw `step缺少extract.obj字段, step:${step}, 配置了extract.expr时, extract.obj必填`
                    }
                    const fields = extract.fields
                    if (!fields || Object.keys(fields) == 0) {
                        throw `step缺少extract.fields字段, step:${step}`
                    }
                }
                if (!stepDefinition.iterator) {
                    throw `step缺少iterator字段, step:${step}, type=fetchHtml时, iterator必填`
                }
                if (!stepDefinition.iterator.collection) {
                    throw `step缺少iterator.collection字段, step:${step}`
                }
                if (!stepDefinition.iterator.step) {
                    throw `step缺少iterator.step字段, step:${step}`
                }
            }
            if (stepDefinition.type == "download") {
                if (!stepDefinition.url) {
                    throw `step缺少url字段, step:${step}`
                }
                if (!stepDefinition.fileName) {
                    throw `step缺少fileName字段, step:${step}`
                }
            }
        }
    }

    execute() {
        const steps = Object.keys(this.definition)
        // 第一个steo作为最开始的步骤
        const entryStep = steps[0]
        this.doExecuteStep(entryStep, null)
    }

    doExecuteStep(step, param) {
        const stepDefinition = this.definition[step]
        const _this = this
        if (stepDefinition.type == "fetchHtml") {
            // 第1个step上的url是实际的url, 后面的step上的url是一个取url的表达式
            const url = param == null ? stepDefinition.url : eval(stepDefinition.url)
            GM_xmlhttpRequest({
                method: "GET",
                url: url,
                onerror: err => {
                    console.error(`请求失败, url:${url}`, err)
                },
                onload: response => {
                    const doc = _this.domParser.parseFromString(response.responseText, 'text/html')
                    const extractResult = _this.getExtractResult(doc, stepDefinition, param)
                    console.log(`${step}, extractResult:`, extractResult)
                    _this.executeIterator(stepDefinition, param, extractResult)

                }
            })
        } else if (stepDefinition.type == "download") {
            const url = eval(stepDefinition.url)
            const fileName = eval(stepDefinition.fileName)
            GM_download({
                url: url,
                name: fileName,
                saveAs: true,
                onload: result => {
                    console.log(`下载完成, 文件名:${fileName}, url:${url}`, result)
                },
                onprogress: progress => {
                    console.log('已下载: ' + progress.loaded + ' 字节, 总大小: ' + (progress.totalSize ? progress.totalSize : '未知'));
                },
                onerror: err => {
                    console.error(`下载失败, 文件名:${fileName}, url:${url}`, err)
                }
            })
        } else if (stepDefinition.type == "downloadPreview") {
            const url = eval(stepDefinition.url)
            const fileName = eval(stepDefinition.fileName)
            console.log(`fileName:${fileName}, url:${url}`)
        }
    }

    getExtractResult(doc, stepDefinition, param) {
        const extractResult = {}
        for (const extract of stepDefinition.extract) {
            const fileds = extract.fields
            if (extract.expr) {
                // 配置了extract.expr, 该表达式提取到的结果是一个数组
                const list = []
                const listFieldName = extract.obj
                extractResult[listFieldName] = list
                const items = eval(extract.expr)
                items.each((index, item) => {
                    const listItem = {}
                    for (const fieldName in fileds) {
                        const selector = fileds[fieldName]
                        const result = eval(selector).replace(/\n/g, "").trim()
                        listItem[fieldName] = result
                    }
                    list.push(listItem)
                })
            } else {
                // 提取非数组字段
                for (const fieldName in fileds) {
                    const selector = fileds[fieldName]
                    const result = eval(selector).replace(/\n/g, "").trim()
                    extractResult[fieldName] = result
                }
            }
        }
        return extractResult
    }

    executeIterator(stepDefinition, param, extract) {
        const iterator = stepDefinition.iterator
        const nextStep = iterator.step
        const collection = eval(iterator.collection)
        let index = 0
        for (const collectionItem of collection) {
            const nextParam = Object.assign({}, param, extract, collectionItem)
            if (iterator.indexVar) {
                nextParam[iterator.indexVar] = index++
            }
            this.doExecuteStep(nextStep, nextParam)
        }
    }
}