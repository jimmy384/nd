package jimmy.practice.spd.component.user.orchestration.vo;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 服务编排的任务定义
 *
 * @author jimmy
 */
@Data
public class TaskDefinition {
    private String type;
    private String name;
    private String template;
    private Result result;

    /**
     * 服务编排任务的结果定义
     *
     * @author jimmy
     */
    @Data
    public static class Result {
        private String type;
        private String key;
        private String jsonPath;
        private String script;

        public String getType() {
            return StringUtils.defaultIfBlank(type, "json");
        }
    }

}
