package jimmy.practice.spd.component.storage.fix;

import jimmy.practice.spd.component.storage.fix.vo.ApproveLogVO;
import jimmy.practice.spd.component.storage.fix.vo.Pair;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ApproveLogChecker {

    public static void main(String[] args) throws Exception {
        ApproveLogStorage storage = new ApproveLogStorage(getApproveLogFromProd());

        List<ApproveLogVO> checkList = getCheckList();

        List<Pair> notSameList = new ArrayList<>();
        List<ApproveLogVO> notFoundList = new ArrayList<>();
        for (ApproveLogVO toCheckLog : checkList) {
            ApproveLogVO foundApproveLog = storage.findApproveLog(toCheckLog);
            if (foundApproveLog != null) {
                if (!toCheckLog.compare(foundApproveLog)) {
                    notSameList.add(new Pair(toCheckLog, foundApproveLog));
                }
            } else {
                notFoundList.add(toCheckLog);
            }
        }

        System.out.println();
        System.out.println("不相同的记录");
        for (int i = 0; i < notSameList.size(); i++) {
            Pair pair = notSameList.get(i);
            System.out.println(i);
            System.out.println(pair.getActual());
            System.out.println(pair.getExpected());
        }

        System.out.println();
        System.out.println("找不到的记录");
        for (ApproveLogVO approveLog : notFoundList) {
            System.out.println(approveLog);
        }
    }

    private static List<ApproveLogVO> getCheckList() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<ApproveLogVO> checkList = new ArrayList<>();
        ApproveLogVO myApproveLogVO1 = new ApproveLogVO();
        myApproveLogVO1.setRouteId("102");
        myApproveLogVO1.setApproveDate(sdf.parse("2023-10-09 13:00:02"));
        myApproveLogVO1.setApproveType("type2");
        checkList.add(myApproveLogVO1);

        ApproveLogVO myApproveLogVO2 = new ApproveLogVO();
        myApproveLogVO2.setRouteId("103");
        myApproveLogVO2.setApproveDate(sdf.parse("2023-10-09 14:00:00"));
        myApproveLogVO2.setApproveType("type1");
        checkList.add(myApproveLogVO2);

        ApproveLogVO myApproveLogVO3 = new ApproveLogVO();
        myApproveLogVO3.setRouteId("104");
        myApproveLogVO3.setApproveDate(sdf.parse("2023-10-09 14:00:00"));
        myApproveLogVO3.setApproveType("type4");
        checkList.add(myApproveLogVO3);
        return checkList;
    }

    private static List<ApproveLogVO> getApproveLogFromProd() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        List<ApproveLogVO> logFromProd = new ArrayList<>();
        ApproveLogVO approveLogVO1 = new ApproveLogVO();
        approveLogVO1.setRouteId("101");
        approveLogVO1.setApproveDate(sdf.parse("2023-10-09 12:00:00"));
        approveLogVO1.setApproveType("type1");
        logFromProd.add(approveLogVO1);

        ApproveLogVO approveLogVO2 = new ApproveLogVO();
        approveLogVO2.setRouteId("102");
        approveLogVO2.setApproveDate(sdf.parse("2023-10-09 13:00:00"));
        approveLogVO2.setApproveType("type2");
        logFromProd.add(approveLogVO2);

        ApproveLogVO approveLogVO3 = new ApproveLogVO();
        approveLogVO3.setRouteId("103");
        approveLogVO3.setApproveDate(sdf.parse("2023-10-09 14:00:00"));
        approveLogVO3.setApproveType("type3");
        logFromProd.add(approveLogVO3);
        return logFromProd;
    }

}
