package jimmy.practice.spd.component.user.cookie;

import cn.hutool.core.util.RuntimeUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * 读取本地浏览器的数据，封装hack-browser-data命令行工具
 *
 * @author jimmy
 */
@Slf4j
@Getter
public class HackBrowserData {
    public static final String DEFAULT_EXECUTABLE_PATH = "/Users/jimmy/Downloads/1/hack-browser-data";
    private final String browserType;
    private final String executablePath;
    private final Map<String, String> pathMapping;

    public HackBrowserData(String browserType) {
        this(browserType, DEFAULT_EXECUTABLE_PATH, Collections.emptyMap());
    }

    public HackBrowserData(String browserType, String executableFilePath, Map<String, String> pathMapping) {
        Preconditions.checkArgument(StringUtils.isNotBlank(browserType));
        Preconditions.checkArgument(StringUtils.isNotBlank(executableFilePath));
        Preconditions.checkNotNull(pathMapping);
        this.browserType = browserType;
        this.executablePath = executableFilePath;
        this.pathMapping = pathMapping;
    }

    /**
     * 读取默认用户数据目录的数据
     */
    public void dump() {
        this.dump(null);
    }

    /**
     * 读取指定用户数据目录的数据
     */
    public void dump(String userDataPath) {
        String resultPath = getResultPath(userDataPath);
        new File(resultPath).mkdirs();
        StringBuilder cmdBuilder = new StringBuilder();
        cmdBuilder.append(executablePath)
                .append(" -b ").append(browserType)
                .append(" -f json")
                .append(" --dir ").append(resultPath);
        if (StringUtils.isNotBlank(userDataPath)) {
            cmdBuilder.append(" -p ").append(userDataPath);
        }
        String cmd = cmdBuilder.toString();
        log.info("HackBrowserData cmd:{}", cmd);
        String logs = RuntimeUtil.execForStr(cmd);
        log.info(logs);
    }

    /**
     * 获取默认用户数据目录对应的dump目录路径
     *
     * @return dump目录路径
     */
    public String getResultPath() {
        return getResultPath(null);
    }

    /**
     * 获取指定用户数据目录对应的dump目录路径
     *
     * @return dump目录路径
     */
    public String getResultPath(String userDataPath) {
        String defaultResultPath = FilenameUtils.concat(FilenameUtils.getFullPath(executablePath), browserType);
        return pathMapping.getOrDefault(userDataPath, defaultResultPath);
    }
}
