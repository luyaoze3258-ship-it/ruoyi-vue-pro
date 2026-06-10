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

    public String getTaskByExternalId(String externalId) {
        return exchange(buildGlobalConfig(), "/api/v1/approval/tasks/by-external/" + externalId, HttpMethod.GET, null, null).getBody();
    }

    public void submitBusinessResult(String guanlanTaskId, BpmAiApprovalBusinessResultReqDTO reqDTO) {
        submitBusinessResult(guanlanTaskId, reqDTO, buildGlobalConfig());
    }

    public void submitBusinessResult(String guanlanTaskId, BpmAiApprovalBusinessResultReqDTO reqDTO, Config config) {
        exchange(config, "/api/v1/approval/tasks/" + guanlanTaskId + "/business-result", HttpMethod.POST, reqDTO, null);
    }

    private ResponseEntity<String> exchange(Config config, String path, HttpMethod method, Object body, String idempotencyKey) {
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

}
