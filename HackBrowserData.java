package jimmy.practice.basic.common.utils.cookie;

import cn.hutool.core.util.RuntimeUtil;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Collections;
import java.util.Map;

@Slf4j
public class HackBrowserData {
    public static final String DEFAULT_EXECUTABLE_PATH = "/Users/jimmy/Downloads/1/hack-browser-data";
    private String browserType;
    private String executablePath;
    private Map<String, String> pathMapping;

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

    public void dump() {
        this.dump(null);
    }

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

    public String getResultPath() {
        return getResultPath(null);
    }

    public String getResultPath(String userDataPath) {
        String defaultResultPath = FilenameUtils.concat(FilenameUtils.getFullPath(executablePath), browserType);
        return pathMapping.getOrDefault(userDataPath, defaultResultPath);
    }
}
