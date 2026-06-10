package cn.iocoder.yudao.module.bpm.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 观澜 AI 审批回调。
 */
@Data
public class BpmAiApprovalCallbackReqDTO {

    @JsonProperty("callback_id")
    @JsonAlias("callbackId")
    private String callbackId;

    @JsonProperty("task_id")
    @JsonAlias("taskId")
    private String taskId;

    @JsonProperty("external_id")
    @JsonAlias("externalId")
    private String externalId;

    private String status;

    private String verdict;

    private String opinion;

}
