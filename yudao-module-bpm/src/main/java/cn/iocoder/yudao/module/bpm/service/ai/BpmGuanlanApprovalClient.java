package cn.iocoder.yudao.module.bpm.service.ai;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalBusinessResultReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitRespDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
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
        Map<String, Object> body = new HashMap<>();
        body.put("external_id", reqDTO.getExternalId());
        body.put("document", reqDTO.getDocument());
        ResponseEntity<String> response = exchange("/api/v1/approval/submit", HttpMethod.POST, body,
                reqDTO.getExternalId());
        GuanlanSubmitResponse submitResponse = JsonUtils.parseObject(response.getBody(), GuanlanSubmitResponse.class);
        BpmAiApprovalSubmitRespDTO result = new BpmAiApprovalSubmitRespDTO();
        result.setTaskId(submitResponse != null ? submitResponse.getTaskId() : null);
        result.setStatus(submitResponse != null ? submitResponse.getStatus() : null);
        return result;
    }

    public String getTask(String taskId) {
        return exchange("/api/v1/approval/tasks/" + taskId, HttpMethod.GET, null, null).getBody();
    }

    public String getTaskByExternalId(String externalId) {
        return exchange("/api/v1/approval/tasks/by-external-id/" + externalId, HttpMethod.GET, null, null).getBody();
    }

    public void submitBusinessResult(String guanlanTaskId, BpmAiApprovalBusinessResultReqDTO reqDTO) {
        exchange("/api/v1/approval/tasks/" + guanlanTaskId + "/business-result", HttpMethod.POST, reqDTO, null);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, Object body, String idempotencyKey) {
        String baseUrl = properties.getGuanlan().getBaseUrl();
        if (StrUtil.isBlank(baseUrl)) {
            throw new IllegalStateException("Guanlan baseUrl is blank");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        if (StrUtil.isNotBlank(properties.getGuanlan().getApiKey())) {
            headers.add("X-API-Key", properties.getGuanlan().getApiKey());
        }
        if (StrUtil.isNotBlank(idempotencyKey)) {
            headers.add("Idempotency-Key", idempotencyKey);
        }
        String url = StrUtil.removeSuffix(baseUrl, "/") + path;
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    @Data
    private static class GuanlanSubmitResponse {

        @JsonProperty("task_id")
        private String taskId;

        private String status;

    }

}
