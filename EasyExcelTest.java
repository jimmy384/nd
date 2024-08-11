package jimmy.practice.basic;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.util.ListUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

@Slf4j
class EasyExcelTest {

    @Test
    void test() {
        System.out.println("test");
        String filePath = "/Users/jimmy/Downloads/test.xlsx";
        try (ExcelReader excelReader = EasyExcel.read(filePath).build()) {
            ReadSheet readSheet1 = EasyExcel.readSheet("Sheet1").head(ApiPubSub.class).headRowNumber(1).registerReadListener(new DemoDataListener()).build();
            ReadSheet readSheet2 = EasyExcel.readSheet("Sheet2").head(ApiPubSub.class).headRowNumber(2).registerReadListener(new DemoDataListener()).build();
            excelReader.read(readSheet1, readSheet2);
        }
    }

    @Data
    public static class ApiMeta {
        @ExcelProperty("登记人")
        private String userName;
        @ExcelProperty("登记时间")
        private Date time;
        @ExcelProperty("资源ID")
        private String assetId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class ApiPubSub extends ApiMeta {
        @ExcelProperty("请求方式")
        private String method;
        @ExcelProperty("请求路径")
        private String path;
    }


    public class DemoDataListener implements ReadListener<ApiPubSub> {
        /**
         * 每隔5条存储数据库，实际使用中可以100条，然后清理list ，方便内存回收
         */
        private static final int BATCH_COUNT = 100;
        /**
         * 缓存的数据
         */
        private List<ApiPubSub> cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

        @Override
        public void invoke(ApiPubSub data, AnalysisContext context) {
            log.info("读取到数据:" + data);
            cachedDataList.add(data);
        }

        /**
         * 所有数据解析完成了 都会来调用
         *
         * @param context
         */
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {

            log.info("所有数据解析完成！");
        }

    }
}
