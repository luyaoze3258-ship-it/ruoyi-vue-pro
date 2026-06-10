package cn.iocoder.yudao.module.bpm.service.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 回灌观澜的业务终态。
 */
@Data
public class BpmAiApprovalBusinessResultReqDTO {

    @JsonProperty("final_verdict")
    private String finalVerdict;

    @JsonProperty("final_opinion")
    private String finalOpinion;

    @JsonProperty("process_instance_id")
    private String processInstanceId;

    @JsonProperty("task_id")
    private String taskId;

}
