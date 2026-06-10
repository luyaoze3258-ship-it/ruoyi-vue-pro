package cn.iocoder.yudao.module.bpm.service.ai;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalBusinessResultReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitRespDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 观澜审批平台 API Client。
 */
@Component
public class BpmGuanlanApprovalClient {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private BpmAiApprovalProperties properties;

    public BpmAiApprovalSubmitRespDTO submit(BpmAiApprovalSubmitReqDTO reqDTO) {
        return submit(reqDTO, buildGlobalConfig());
    }

    public BpmAiApprovalSubmitRespDTO submit(BpmAiApprovalSubmitReqDTO reqDTO, Config config) {
        Map<String, Object> body = new HashMap<>();
        body.put("external_id", reqDTO.getExternalId());
        body.put("document", reqDTO.getDocument());
        ResponseEntity<String> response = exchange(config, "/api/v1/approval/submit", HttpMethod.POST, body,
                reqDTO.getExternalId());
        BpmAiApprovalSubmitRespDTO result = new BpmAiApprovalSubmitRespDTO();
        result.setTaskId(parseSubmitTaskId(response.getBody()));
        result.setStatus(parseSubmitStatus(response.getBody()));
        return result;
    }

    public String getTask(String taskId) {
        return exchange(buildGlobalConfig(), "/api/v1/approval/tasks/" + taskId, HttpMethod.GET, null, null).getBody();
    }

    public TaskResult getTask(String taskId, Config config) {
        return parseTaskResult(exchange(config, "/api/v1/approval/tasks/" + taskId, HttpMethod.GET, null, null)
                .getBody());
    }

    public String getTaskByExternalId(String externalId) {
        return exchange(buildGlobalConfig(), "/api/v1/approval/tasks/by-external/" + externalId, HttpMethod.GET, null, null).getBody();
    }

    public void submitBusinessResult(String guanlanTaskId, BpmAiApprovalBusinessResultReqDTO reqDTO) {
        submitBusinessResult(guanlanTaskId, reqDTO, buildGlobalConfig());
    }

    public void submitBusinessResult(String guanlanTaskId, BpmAiApprovalBusinessResultReqDTO reqDTO, Config config) {
        exchange(config, "/api/v1/approval/tasks/" + guanlanTaskId + "/business-result", HttpMethod.POST, reqDTO, null);
    }

    public ChatResult chat(String question, Map<String, Object> context, Config config) {
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", buildChatPrompt(question, context));

        Map<String, Object> input = new HashMap<>();
        input.put("messages", List.of(userMessage));

        Map<String, Object> body = new HashMap<>();
        body.put("input", input);
        body.put("context", context);
        body.put("metadata", Map.of("source", "ruoyi-vue-pro-bpm-ai-approval"));
        body.put("on_completion", "delete");

        String csrfToken = newChatCsrfToken();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-Token", csrfToken);
        headers.add(HttpHeaders.COOKIE, "csrf_token=" + csrfToken);
        String responseBody = exchange(config, "/api/runs/wait", HttpMethod.POST, body, null, headers).getBody();
        return new ChatResult().setAnswer(parseChatAnswer(responseBody));
    }

    String buildChatPrompt(String question, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是当前审批单据的 AI 审批助手。请只基于下面的当前单据上下文回答用户问题，")
                .append("不要要求用户重新上传单据。回答要简短、直接、面向审批人。\n\n");
        prompt.append("用户问题：").append(StrUtil.blankToDefault(question, "")).append("\n\n");
        prompt.append("当前单据上下文：\n");
        appendContextLine(prompt, "流程实例", context.get("processInstanceId"));
        appendContextLine(prompt, "审批任务", context.get("taskName"));
        appendContextLine(prompt, "观澜任务", context.get("guanlanTaskId"));
        appendContextLine(prompt, "智能体", context.get("agentName"));
        appendContextLine(prompt, "AI结论", context.get("conclusion"));
        appendContextLine(prompt, "采纳AI结论", context.get("adoptEnabled"));
        appendContextLine(prompt, "表单字段", formatFormVariables(context.get("formVariables")));
        appendContextLine(prompt, "完整分析", context.get("opinion"));
        return prompt.toString();
    }

    private static void appendContextLine(StringBuilder prompt, String label, Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return;
        }
        prompt.append("- ").append(label).append("：").append(value).append("\n");
    }

    private static String formatFormVariables(Object formVariables) {
        if (!(formVariables instanceof Map)) {
            return null;
        }
        Map<?, ?> variableMap = (Map<?, ?>) formVariables;
        if (variableMap.isEmpty()) {
            return null;
        }
        return variableMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("；"));
    }

    private ResponseEntity<String> exchange(Config config, String path, HttpMethod method, Object body, String idempotencyKey) {
        return exchange(config, path, method, body, idempotencyKey, null);
    }

    private ResponseEntity<String> exchange(Config config, String path, HttpMethod method, Object body,
                                            String idempotencyKey, HttpHeaders customHeaders) {
        String baseUrl = config.getBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            throw new IllegalStateException("Guanlan baseUrl is blank");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        if (StrUtil.isNotBlank(config.getApiKey())) {
            headers.add("X-API-Key", config.getApiKey());
        }
        if (StrUtil.isNotBlank(idempotencyKey)) {
            headers.add("Idempotency-Key", idempotencyKey);
        }
        if (customHeaders != null && !customHeaders.isEmpty()) {
            customHeaders.forEach((name, values) -> values.forEach(value -> headers.add(name, value)));
        }
        String url = StrUtil.removeSuffix(baseUrl, "/") + path;
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private Config buildGlobalConfig() {
        return new Config()
                .setBaseUrl(properties.getGuanlan().getBaseUrl())
                .setApiKey(properties.getGuanlan().getApiKey());
    }

    String parseSubmitTaskId(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return null;
        }
        JsonNode rootNode = JsonUtils.parseTree(responseBody);
        String taskId = getText(rootNode, "task_id", "taskId", "id");
        if (StrUtil.isNotBlank(taskId)) {
            return taskId;
        }
        JsonNode dataNode = rootNode.get("data");
        return dataNode == null ? null : getText(dataNode, "task_id", "taskId", "id");
    }

    private String parseSubmitStatus(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return null;
        }
        JsonNode rootNode = JsonUtils.parseTree(responseBody);
        String status = getText(rootNode, "status");
        if (StrUtil.isNotBlank(status)) {
            return status;
        }
        JsonNode dataNode = rootNode.get("data");
        return dataNode == null ? null : getText(dataNode, "status");
    }

    TaskResult parseTaskResult(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return new TaskResult();
        }
        JsonNode rootNode = JsonUtils.parseTree(responseBody);
        JsonNode taskNode = rootNode.has("data") && rootNode.get("data").isObject() ? rootNode.get("data") : rootNode;
        return new TaskResult()
                .setTaskId(getText(taskNode, "task_id", "taskId", "id"))
                .setExternalId(getText(taskNode, "external_id", "externalId"))
                .setStatus(getText(taskNode, "status"))
                .setVerdict(getText(taskNode, "verdict"))
                .setOpinion(getText(taskNode, "opinion"));
    }

    String parseChatAnswer(String responseBody) {
        if (StrUtil.isBlank(responseBody)) {
            return null;
        }
        JsonNode rootNode = JsonUtils.parseTree(responseBody);
        String answer = getText(rootNode, "answer", "content", "output", "text");
        if (StrUtil.isNotBlank(answer)) {
            return answer;
        }
        answer = findLastAssistantMessage(rootNode.get("messages"));
        if (StrUtil.isNotBlank(answer)) {
            return answer;
        }
        JsonNode valuesNode = rootNode.get("values");
        if (valuesNode != null) {
            answer = getText(valuesNode, "answer", "content", "output", "text");
            if (StrUtil.isNotBlank(answer)) {
                return answer;
            }
            answer = findLastAssistantMessage(valuesNode.get("messages"));
            if (StrUtil.isNotBlank(answer)) {
                return answer;
            }
        }
        JsonNode dataNode = rootNode.get("data");
        if (dataNode != null) {
            answer = getText(dataNode, "answer", "content", "output", "text");
            if (StrUtil.isNotBlank(answer)) {
                return answer;
            }
            answer = findLastAssistantMessage(dataNode.get("messages"));
            if (StrUtil.isNotBlank(answer)) {
                return answer;
            }
        }
        return null;
    }

    private static String findLastAssistantMessage(JsonNode messagesNode) {
        if (messagesNode == null || !messagesNode.isArray()) {
            return null;
        }
        String fallback = null;
        for (JsonNode messageNode : messagesNode) {
            String content = getText(messageNode, "content", "text");
            if (StrUtil.isBlank(content)) {
                continue;
            }
            fallback = content;
            String role = getText(messageNode, "role", "type");
            if (StrUtil.equalsAnyIgnoreCase(role, "assistant", "ai")) {
                return content;
            }
        }
        return fallback;
    }

    String newChatCsrfToken() {
        return UUID.randomUUID().toString();
    }

    HttpHeaders buildChatHeadersForTest() {
        String csrfToken = newChatCsrfToken();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-CSRF-Token", csrfToken);
        headers.add(HttpHeaders.COOKIE, "csrf_token=" + csrfToken);
        return headers;
    }

    private static String getText(JsonNode node, String... fields) {
        if (node == null || !node.isObject()) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isValueNode() && StrUtil.isNotBlank(value.asText())) {
                return value.asText();
            }
        }
        return null;
    }

    @Data
    public static class Config {

        private String baseUrl;

        private String apiKey;

    }

    @Data
    public static class TaskResult {

        private String taskId;

        private String externalId;

        private String status;

        private String verdict;

        private String opinion;

    }

    @Data
    public static class ChatResult {

        private String answer;

    }

}
