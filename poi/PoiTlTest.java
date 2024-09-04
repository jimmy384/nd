package jimmy.practice.spd.component.user.poi;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.PictureRenderData;
import com.deepoove.poi.data.Pictures;
import com.deepoove.poi.data.TableRenderData;
import com.deepoove.poi.data.Tables;
import com.deepoove.poi.data.style.BorderStyle;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.google.common.collect.Lists;
import jimmy.practice.basic.poi.vo.ConfigCenterVO;
import jimmy.practice.basic.poi.vo.ReleaseData;
import jimmy.practice.basic.poi.vo.StarlingParameterVO;
import jimmy.practice.basic.poi.vo.UserStoryVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

@Slf4j
class PoiTlTest {

    @Test
    void test() throws IOException {
        String filePath = "/Users/jimmy/Downloads/test.docx";
        String targetFilePath = "/Users/jimmy/Downloads/testTarget.docx";

        ReleaseData releaseData = getReleaseData();


        //核心API采用了极简设计，只需要一行代码
        HashMap<String, Object> data = new HashMap<>();
        data.put("releaseData", releaseData);
        data.put("key", "test");

        PictureRenderData pictureRednerData = Pictures.ofUrl("https://i0.hdslb.com/bfs/archive/c8fd97a40bf79f03e7b76cbc87236f612caef7b2.png").size(800, 450).create();
        data.put("picture", pictureRednerData);

        TableRenderData tableRenderData = Tables.of(new String[][]{
                new String[]{"名称", "职业"},
                new String[]{"Song name2", "Artist3"},
                new String[]{"Song name2", "Artist3"}
        }).border(BorderStyle.DEFAULT).create();
        data.put("table", tableRenderData);

        LoopRowTableRenderPolicy hackLoopTableRenderPolicy = new LoopRowTableRenderPolicy();
        Configure config = Configure.builder().bind("usList", hackLoopTableRenderPolicy).build();
        XWPFTemplate.compile(filePath, config).render(data).writeToFile(targetFilePath);
    }

    private static ReleaseData getReleaseData() {
        ReleaseData releaseData = new ReleaseData();
        releaseData.setPi("20240917");
        UserStoryVO userStoryVO1 = new UserStoryVO();
        userStoryVO1.setNumber("US1234");
        userStoryVO1.setName("需求1");
        UserStoryVO userStoryVO2 = new UserStoryVO();
        userStoryVO2.setNumber("US5678");
        userStoryVO2.setName("需求2");
        releaseData.setUsList(Lists.newArrayList(userStoryVO1, userStoryVO2));
        releaseData.setHasLookup(true);
        releaseData.setHasI18n(true);
        releaseData.setHasDict(true);
        StarlingParameterVO starlingParameterVO1 = new StarlingParameterVO();
        starlingParameterVO1.setKey("key1");
        starlingParameterVO1.setValue("value1");
        StarlingParameterVO starlingParameterVO2 = new StarlingParameterVO();
        starlingParameterVO2.setKey("key1");
        starlingParameterVO2.setValue("value1");
        releaseData.setStarlingParameterVOs(Lists.newArrayList(starlingParameterVO1, starlingParameterVO2));
        ConfigCenterVO configCenterVO1 = new ConfigCenterVO();
        configCenterVO1.setKey("key1");
        configCenterVO1.setValue("value1");
        configCenterVO1.setTag("tag1");
        configCenterVO1.setUnit("unit1");
        ConfigCenterVO configCenterVO2 = new ConfigCenterVO();
        configCenterVO2.setKey("key1");
        configCenterVO2.setValue("value1");
        configCenterVO2.setTag("tag2");
        configCenterVO2.setUnit("unit2");
        releaseData.setConfigCenterVOS(Lists.newArrayList(configCenterVO1, configCenterVO2));
        releaseData.setPublishApiVOS(Lists.newArrayList());
        releaseData.setSubscribeApiVOs(Lists.newArrayList());
        releaseData.setVirtualUserVOS(Lists.newArrayList());
        releaseData.setDeployUnitVOS(Lists.newArrayList());
        releaseData.setDbScriptVOS(Lists.newArrayList());
        return releaseData;
    }

}
