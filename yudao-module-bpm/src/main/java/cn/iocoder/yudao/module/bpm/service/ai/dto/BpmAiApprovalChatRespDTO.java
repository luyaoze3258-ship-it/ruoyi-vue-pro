package cn.iocoder.yudao.module.bpm.service.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - BPM AI 审批对话 Response DTO")
@Data
public class BpmAiApprovalChatRespDTO {

    @Schema(description = "回答来源", example = "local")
    private String source;

    @Schema(description = "回答内容")
    private String answer;

}
