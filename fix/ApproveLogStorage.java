package jimmy.practice.spd.component.storage.fix;

import jimmy.practice.spd.component.storage.fix.vo.ApproveLogVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApproveLogStorage {
    private final Map<String, ApproveLogVO> storage = new HashMap<>();

    public ApproveLogStorage(List<ApproveLogVO> approveLogs) {
        for (ApproveLogVO approveLog : approveLogs) {
            addToMap(storage, approveLog);
        }
    }

    public ApproveLogVO findApproveLog(ApproveLogVO targetApproveLog) {
        List<String> keys = targetApproveLog.keys();
        for (String key : keys) {
            ApproveLogVO approveLog = storage.get(key);
            if (approveLog != null) {
                return approveLog;
            }
        }
        return null;
    }

    private static void addToMap(Map<String, ApproveLogVO> prodMap, ApproveLogVO approveLog) {
        List<String> keys = approveLog.keys();
        for (String key : keys) {
            prodMap.put(key, approveLog);
        }
    }
}
