public class DataHolder {
    def detailAll // 大详情对象
    def Map<String, List> lookupMap = new HashMap()
    def Map<String, Object> dataMap = new HashMap()

    public Object getDetailAll(intentionOrderId) {
        if (detailAll == null) {
            // 调用其他子流程, 取到的返回值
            detailAll = callOtherProcess("DataHolder_getDetailAll", [intentionOrderId: intentionOrderId])
        }
        return detailAll
    }

    public List getLookup(classifyCode) {
        def lookupItems = lookupMap.get(classifyCode)
        if (lookupItems == null) {
            // 调用其他子流程, 取到的返回值
            lookupItems = callOtherProcess("DataHolder_getLookup", [classifyCode: classifyCode])
            lookupMap.put(classifyCode, lookupItems)
        }
        return lookupItems
    }

    public void initLookup(classifyCodes) {
        callOtherProcess("DataHolder_initLookup", [classifyCodes: classifyCodes])
    }

    public Object getValue(key) {
        
    }

    public Object setValue(key, value) {
        
    }
}