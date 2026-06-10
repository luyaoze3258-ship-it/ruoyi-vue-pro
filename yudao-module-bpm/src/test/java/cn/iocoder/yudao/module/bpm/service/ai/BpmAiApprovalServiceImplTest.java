package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalTaskDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmAiApprovalTaskMapper;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BpmAiApprovalServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmAiApprovalServiceImpl aiApprovalService;

    @Mock
    private BpmAiApprovalTaskMapper aiApprovalTaskMapper;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private TaskService flowableTaskService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(aiApprovalService, "decisionPolicy", new BpmAiApprovalDecisionPolicy());
    }

    @Test
    public void applySuggestion_shouldWriteTaskReasonAndNotCompleteTask() {
        BpmAiApprovalTaskDO approvalTask = BpmAiApprovalTaskDO.builder()
                .id(10L)
                .taskId("task-1")
                .adoptEnabled(false)
                .build();
        BpmAiApprovalCallbackReqDTO callbackReqDTO = new BpmAiApprovalCallbackReqDTO()
                .setVerdict("green")
                .setOpinion("材料齐全，建议通过");

        aiApprovalService.applyDecision(approvalTask, callbackReqDTO);

        verify(flowableTaskService).setVariableLocal(eq("task-1"), eq(BpmnVariableConstants.TASK_VARIABLE_REASON),
                eq("AI 审批结论：green；材料齐全，建议通过"));
        verify(bpmTaskService, never()).approveTask(eq(null), org.mockito.ArgumentMatchers.any());
        verify(bpmTaskService, never()).rejectTask(eq(null), org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void applyAdoptGreen_shouldApproveWithCurrentTaskAssignee() {
        BpmAiApprovalTaskDO approvalTask = BpmAiApprovalTaskDO.builder()
                .id(10L)
                .taskId("task-1")
                .assigneeUserId(100L)
                .adoptEnabled(true)
                .build();
        BpmAiApprovalCallbackReqDTO callbackReqDTO = new BpmAiApprovalCallbackReqDTO()
                .setVerdict("green")
                .setOpinion("材料齐全，建议通过");
        Task currentTask = org.mockito.Mockito.mock(Task.class);
        when(currentTask.getAssignee()).thenReturn("200");
        when(bpmTaskService.getTask("task-1")).thenReturn(currentTask);

        aiApprovalService.applyDecision(approvalTask, callbackReqDTO);

        verify(bpmTaskService).approveTask(eq(200L), argThat((BpmTaskApproveReqVO reqVO) ->
                "task-1".equals(reqVO.getId())
                        && "AI 审批结论：green；材料齐全，建议通过".equals(reqVO.getReason())));
        verify(bpmTaskService, never()).rejectTask(any(), any());
    }

}
