package jimmy.practice.spd.component.user.orchestration;

import com.alibaba.fastjson2.JSONPath;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jimmy.practice.basic.common.jackson.YamlUtils;
import jimmy.practice.basic.common.utils.cookie.CookieReader;
import jimmy.practice.basic.common.utils.cookie.HackBrowserData;
import jimmy.practice.basic.common.utils.curl.CurlExecutor;
import jimmy.practice.basic.common.utils.freemarker.FreeMarkerUtils;
import jimmy.practice.basic.common.utils.orchestration.vo.OrchestrationDefinition;
import jimmy.practice.basic.common.utils.orchestration.vo.TaskDefinition;
import jimmy.practice.basic.common.utils.orchestration.vo.TaskDefinition.Result;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 服务编排执行器
 *
 * @author jimmy
 */
@Setter
public class Orchestrator {
    private Map<String, Object> defaultParam;
    private PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private String resourceBasePath = "classpath:/orchestration/";
    private CurlExecutor curlExecutor = new CurlExecutor(new CookieReader(new HackBrowserData("edge")));

    public static void main(String[] args) {
        Orchestrator orchestrator = new Orchestrator();
        HashMap<String, Object> defaultParam = new HashMap<>();
        orchestrator.setDefaultParam(defaultParam);
        Map<String, Object> context = orchestrator.execute("test");
        System.out.println(context);
    }

    public Map<String, Object> execute(String name) {
        return this.execute(name, Collections.emptyMap());
    }

    public Map<String, Object> execute(String name, Map<String, Object> param) {
        String resourcePath = resourceBasePath + name + ".yaml";
        Resource resource = resourcePatternResolver.getResource(resourcePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("找不到资源, path: " + resourcePath);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            OrchestrationDefinition orchestrationDefinition = YamlUtils.toBean(inputStream, OrchestrationDefinition.class);
            orchestrationDefinition.validate();
            return doExecute(orchestrationDefinition, param);
        } catch (IOException e) {
            throw new IllegalStateException("读取服务编排定义失败, name:" + name, e);
        }
    }

    private Map<String, Object> doExecute(OrchestrationDefinition orchestrationDefinition, Map<String, Object> param) {
        Map<String, Object> context = prepareContext(orchestrationDefinition, param);
        Map<String, TaskDefinition> tasks = orchestrationDefinition.getTasks();
        for (Entry<String, TaskDefinition> entry : tasks.entrySet()) {
            String taskName = entry.getKey();
            TaskDefinition taskDefinition = entry.getValue();
            taskDefinition.setName(taskName);
            String taskType = taskDefinition.getType();
            switch (taskType) {
                case "curl":
                    handleCurlTask(taskDefinition, context);
                    break;
                case "groovy":
                    handleGroovyTask(taskDefinition, context);
                    break;
                default:
                    break;
            }
        }
        return context;
    }

    private void handleCurlTask(TaskDefinition taskDefinition, Map<String, Object> context) {
        String template = taskDefinition.getTemplate();
        Result result = taskDefinition.getResult();
        String type = result.getType();
        String resultKey = result.getKey();
        String jsonPath = result.getJsonPath();

        String curlCmd = FreeMarkerUtils.render(template, context);
        if ("json".equals(type)) {
            curlExecutor.execute(curlCmd, response -> {
                String body = EntityUtils.toString(response.getEntity());
                Object jsonPathResult = JSONPath.eval(body, jsonPath);
                context.put(resultKey, jsonPathResult);

                executeGroovyScript(taskDefinition, context);
                return null;
            });
        } else if ("groovy".equals(type)) {
            Object scriptResult = executeGroovyScript(taskDefinition, context);
            if (scriptResult != null) {
                context.put(resultKey, scriptResult);
            }
        }
    }

    private void handleGroovyTask(TaskDefinition taskDefinition, Map<String, Object> context) {
        Object scriptResult = executeGroovyScript(taskDefinition, context);
        String resultKey = taskDefinition.getResult().getKey();
        context.put(resultKey, scriptResult);
    }

    private Map<String, Object> prepareContext(OrchestrationDefinition orchestrationDefinition, Map<String, Object> param) {
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> initParam = orchestrationDefinition.getInitParam();
        if (MapUtils.isNotEmpty(initParam)) {
            context.putAll(initParam);
        }
        if (MapUtils.isNotEmpty(defaultParam)) {
            context.putAll(defaultParam);
        }
        if (MapUtils.isNotEmpty(param)) {
            context.putAll(param);
        }
        return context;
    }

    private static Object executeGroovyScript(TaskDefinition taskDefinition, Map<String, Object> context) {
        String script = taskDefinition.getResult().getScript();
        if (StringUtils.isBlank(script)) {
            return null;
        }
        Binding binding = new Binding();
        binding.setProperty("context", context);
        GroovyShell groovyShell = new GroovyShell(binding);
        return groovyShell.evaluate(script, taskDefinition.getName() + ".groovy");
    }
}
