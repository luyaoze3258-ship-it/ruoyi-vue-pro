package cn.iocoder.yudao.module.bpm.service.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - BPM AI 审批详情 Response DTO")
@Data
public class BpmAiApprovalDetailRespDTO {

    @Schema(description = "流程实例编号", example = "9559a6d1-64d8-11f1-816d-5661024b06cf")
    private String processInstanceId;

    @Schema(description = "BPM 任务编号", example = "96dc013a-64d8-11f1-816d-5661024b06cf")
    private String taskId;

    @Schema(description = "任务定义标识", example = "Activity_ai")
    private String taskDefinitionKey;

    @Schema(description = "任务名称", example = "AI 审批节点")
    private String taskName;

    @Schema(description = "观澜任务编号", example = "b44d8518-e521-4efe-acc3-87d8c27c9d43")
    private String guanlanTaskId;

    @Schema(description = "智能体名称", example = "费用审核智能体")
    private String agentName;

    @Schema(description = "AI 结论", example = "yellow")
    private String verdict;

    @Schema(description = "短结论", example = "AI结论：需人工复核")
    private String conclusion;

    @Schema(description = "完整 AI 分析")
    private String opinion;

    @Schema(description = "是否采纳 AI 结论", example = "false")
    private Boolean adoptEnabled;

    @Schema(description = "本地任务状态", example = "3")
    private Integer status;

    @Schema(description = "回调/同步时间")
    private LocalDateTime callbackTime;

}
