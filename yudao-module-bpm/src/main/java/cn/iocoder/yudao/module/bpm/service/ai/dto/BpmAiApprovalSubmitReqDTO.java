package cn.iocoder.yudao.module.bpm.service.ai.dto;

import lombok.Data;

import java.util.Map;

/**
 * 提交给观澜的 AI 审批单据。
 */
@Data
public class BpmAiApprovalSubmitReqDTO {

    private String externalId;

    private String processInstanceId;

    private String processDefinitionId;

    private String taskId;

    private String taskDefinitionKey;

    private String taskName;

    private Long assigneeUserId;

    private Map<String, Object> document;

}
