package cn.iocoder.yudao.module.bpm.enums.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * BPM AI 审批任务状态枚举。
 */
@Getter
@AllArgsConstructor
public enum BpmAiApprovalStatusEnum {

    SUBMITTED(1, "已提交"),
    CALLBACK_RECEIVED(2, "已回调"),
    SUGGESTED(3, "仅建议"),
    AUTO_APPROVED(4, "AI 自动通过"),
    AUTO_REJECTED(5, "AI 自动驳回"),
    BUSINESS_RESULT_SYNCED(6, "业务终态已回灌"),
    SUBMIT_FAILED(7, "提交失败");

    private final Integer status;
    private final String name;

}
