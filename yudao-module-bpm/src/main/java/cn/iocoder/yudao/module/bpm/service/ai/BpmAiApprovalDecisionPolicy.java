package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * AI 审批结论采纳策略。
 */
@Component
public class BpmAiApprovalDecisionPolicy {

    public Decision decide(boolean adoptEnabled, String verdict) {
        if ("green".equals(verdict)) {
            return adoptEnabled ? Decision.approve() : Decision.suggestOnly();
        }
        if ("red".equals(verdict)) {
            return adoptEnabled ? Decision.reject() : Decision.suggestOnly();
        }
        if ("yellow".equals(verdict)) {
            return Decision.suggestOnly();
        }
        throw new IllegalArgumentException("Unsupported AI approval verdict: " + verdict);
    }

    public enum Action {
        SUGGEST_ONLY,
        APPROVE,
        REJECT
    }

    @Data
    @AllArgsConstructor
    public static class Decision {

        private Action action;
        private Integer taskStatus;

        public static Decision suggestOnly() {
            return new Decision(Action.SUGGEST_ONLY, null);
        }

        public static Decision approve() {
            return new Decision(Action.APPROVE, BpmTaskStatusEnum.APPROVE.getStatus());
        }

        public static Decision reject() {
            return new Decision(Action.REJECT, BpmTaskStatusEnum.REJECT.getStatus());
        }

    }

}
