package cn.iocoder.yudao.module.bpm.service.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - BPM AI 审批对话 Request DTO")
@Data
public class BpmAiApprovalChatReqDTO {

    @Schema(description = "流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "9559a6d1-64d8-11f1-816d-5661024b06cf")
    @NotBlank(message = "流程实例编号不能为空")
    private String processInstanceId;

    @Schema(description = "问题", requiredMode = Schema.RequiredMode.REQUIRED, example = "为什么需要人工复核？")
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题不能超过 500 个字符")
    private String question;

}
