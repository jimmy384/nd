package jimmy.practice.spd.component.storage.fix.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class ApproveLogVO {
    private String routeId;
    private Date approveDate;
    private String approveNode;
    private String approveType;
    private String routeInfo;
    private String remark;

    public String key() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return routeId + "_" + sdf.format(approveDate);
    }

    public List<String> keys() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date lefeDate = DateUtils.addSeconds(approveDate, 1);
        Date rightDate = DateUtils.addSeconds(approveDate, -1);
        List<String> keys = new ArrayList<>();
        keys.add(routeId + "_" + sdf.format(approveDate));
        keys.add(routeId + "_" + sdf.format(lefeDate));
        keys.add(routeId + "_" + sdf.format(rightDate));
        return keys;
    }

    public boolean compare(ApproveLogVO other) {
        String thisApproveNode = StringUtils.defaultString(approveNode, "");
        String otherApproveNode = StringUtils.defaultString(other.approveNode, "");

        String thisApproveType = StringUtils.defaultString(approveType, "");
        String otherApproveType = StringUtils.defaultString(other.approveType, "");

        String thisRemark = StringUtils.truncate(StringUtils.defaultString(remark, ""), 10);
        String otherRemark = StringUtils.truncate(StringUtils.defaultString(other.remark, ""), 10);

        String thisRouteInfo = StringUtils.defaultString(routeInfo, "");
        String otherRouteInfo = StringUtils.defaultString(other.routeInfo, "");

        return StringUtils.equals(thisApproveNode, otherApproveNode)
                && StringUtils.equals(thisApproveType, otherApproveType)
                && StringUtils.equals(thisRemark, otherRemark)
                && StringUtils.equals(thisRouteInfo, otherRouteInfo);
    }
}
