package jimmy.practice.basic;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.*;
import com.deepoove.poi.data.style.BorderStyle;
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
        //核心API采用了极简设计，只需要一行代码
        HashMap<String, Object> data = new HashMap<>();
        data.put("title", "poi-tl 模板引擎");

//        PictureRenderData pictureRednerData = Pictures.ofLocal("/Users/jimmy/Downloads/test.png").size(800, 450).create();
        PictureRenderData pictureRednerData = Pictures.ofUrl("https://i0.hdslb.com/bfs/archive/c8fd97a40bf79f03e7b76cbc87236f612caef7b2.png").size(800, 450).create();

        data.put("picture", pictureRednerData);

        TableRenderData tableRenderData = Tables.of(new String[][]{
                new String[]{"名称", "职业"},
                new String[]{"Song name2", "Artist3"},
                new String[]{"Song name2", "Artist3"}
        }).border(BorderStyle.DEFAULT).create();
        data.put("table", tableRenderData);

        XWPFTemplate.compile(filePath).render(data).writeToFile(targetFilePath);
    }

}
