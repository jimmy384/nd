package jimmy.practice.spd.component.user.orchestration.vo;

import com.google.common.base.Preconditions;
import jimmy.practice.basic.common.utils.orchestration.vo.TaskDefinition.Result;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Map.Entry;

/**
 * 服务编排定义
 *
 * @author jimmy
 */
@Data
public class OrchestrationDefinition {
    private Map<String, Object> initParam;
    private Map<String, TaskDefinition> tasks;

    public void validate() {
        Preconditions.checkArgument(MapUtils.isNotEmpty(tasks), "tasks不能为空");
        for (Entry<String, TaskDefinition> entry : tasks.entrySet()) {
            TaskDefinition taskDefinition = entry.getValue();
            Preconditions.checkArgument(StringUtils.isNotBlank(taskDefinition.getType()), "任务类型type不能为空");
            Result result = taskDefinition.getResult();
            Preconditions.checkArgument(result != null, "任务结果result不能为空");
            if ("curl".equals(taskDefinition.getType())) {
                Preconditions.checkArgument(StringUtils.isNotBlank(taskDefinition.getTemplate()), "任务结果template不能为空");
                if ("json".equals(result.getType())) {
                    Preconditions.checkArgument(StringUtils.isNotBlank(result.getKey()), "任务结果result.key不能为空");
                    Preconditions.checkArgument(StringUtils.isNotBlank(result.getJsonPath()), "任务结果result.jsonPath不能为空");
                }
            }
            if ("stream".equals(result.getType())) {
                Preconditions.checkArgument(StringUtils.isNotBlank(result.getScript()), "任务结果result.script不能为空");
            }
        }
    }
}
