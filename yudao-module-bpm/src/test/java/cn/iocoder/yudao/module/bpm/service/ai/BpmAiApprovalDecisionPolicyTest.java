package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.enums.task.BpmTaskStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BpmAiApprovalDecisionPolicyTest extends BaseMockitoUnitTest {

    private final BpmAiApprovalDecisionPolicy policy = new BpmAiApprovalDecisionPolicy();

    @Test
    public void decide_notAdopt_greenAsSuggestion() {
        BpmAiApprovalDecisionPolicy.Decision decision = policy.decide(false, "green");

        assertEquals(BpmAiApprovalDecisionPolicy.Action.SUGGEST_ONLY, decision.getAction());
        assertNull(decision.getTaskStatus());
    }

    @Test
    public void decide_adopt_greenApproves() {
        BpmAiApprovalDecisionPolicy.Decision decision = policy.decide(true, "green");

        assertEquals(BpmAiApprovalDecisionPolicy.Action.APPROVE, decision.getAction());
        assertEquals(BpmTaskStatusEnum.APPROVE.getStatus(), decision.getTaskStatus());
    }

    @Test
    public void decide_adopt_redRejects() {
        BpmAiApprovalDecisionPolicy.Decision decision = policy.decide(true, "red");

        assertEquals(BpmAiApprovalDecisionPolicy.Action.REJECT, decision.getAction());
        assertEquals(BpmTaskStatusEnum.REJECT.getStatus(), decision.getTaskStatus());
    }

    @Test
    public void decide_adopt_yellowAsSuggestion() {
        BpmAiApprovalDecisionPolicy.Decision decision = policy.decide(true, "yellow");

        assertEquals(BpmAiApprovalDecisionPolicy.Action.SUGGEST_ONLY, decision.getAction());
        assertNull(decision.getTaskStatus());
    }

    @Test
    public void decide_unknownVerdictRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.decide(true, "blue"));
    }
}
