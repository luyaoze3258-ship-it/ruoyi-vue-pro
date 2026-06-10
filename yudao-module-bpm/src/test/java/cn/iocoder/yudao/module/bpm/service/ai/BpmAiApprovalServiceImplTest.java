package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalTaskDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmAiApprovalTaskMapper;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitRespDTO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.runtime.ProcessInstance;
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
import static org.mockito.Mockito.*;

public class BpmAiApprovalServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmAiApprovalServiceImpl aiApprovalService;

    @Mock
    private BpmAiApprovalTaskMapper aiApprovalTaskMapper;
    @Mock
    private BpmTaskService bpmTaskService;
    @Mock
    private TaskService flowableTaskService;
    private FakeGuanlanApprovalClient guanlanApprovalClient;

    @BeforeEach
    public void setUp() {
        guanlanApprovalClient = new FakeGuanlanApprovalClient();
        ReflectionTestUtils.setField(aiApprovalService, "guanlanApprovalClient", guanlanApprovalClient);
        ReflectionTestUtils.setField(aiApprovalService, "decisionPolicy", new BpmAiApprovalDecisionPolicy());
        BpmAiApprovalProperties properties = new BpmAiApprovalProperties();
        properties.setEnabled(true);
        ReflectionTestUtils.setField(aiApprovalService, "properties", properties);
    }

    @Test
    public void submitTaskIfEnabled_shouldUseNodeGuanlanConfig() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn("process-1");
        when(processInstance.getProcessDefinitionId()).thenReturn("definition-1");
        when(processInstance.getBusinessKey()).thenReturn("biz-1");
        when(processInstance.getProcessVariables()).thenReturn(java.util.Collections.singletonMap("amount", 100));
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getTaskDefinitionKey()).thenReturn("Activity_ai");
        when(task.getName()).thenReturn("AI 审批节点");
        when(task.getAssignee()).thenReturn("1");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        UserTask userTask = new UserTask();
        BpmnModelUtils.addAiApprovalSetting(new BpmSimpleModelNodeVO.AiApprovalSetting()
                .setEnable(true)
                .setAdoptResult(true)
                .setAgentName("观澜费用审核")
                .setBaseUrl("https://guanlan.guixucloud.com/")
                .setApiKey("node-api-key"), userTask);
        guanlanApprovalClient.submitRespDTO = new BpmAiApprovalSubmitRespDTO()
                .setTaskId("guanlan-task-1").setStatus("accepted");

        aiApprovalService.submitTaskIfEnabled(processInstance, task, userTask);

        verify(aiApprovalTaskMapper).insert(argThat((BpmAiApprovalTaskDO approvalTask) ->
                "观澜费用审核".equals(approvalTask.getGuanlanAgentName())
                        && "https://guanlan.guixucloud.com/".equals(approvalTask.getGuanlanBaseUrl())
                        && "node-api-key".equals(approvalTask.getGuanlanApiKey())));
        org.junit.jupiter.api.Assertions.assertEquals("https://guanlan.guixucloud.com/",
                guanlanApprovalClient.submitConfig.getBaseUrl());
        org.junit.jupiter.api.Assertions.assertEquals("node-api-key", guanlanApprovalClient.submitConfig.getApiKey());
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

    private static class FakeGuanlanApprovalClient extends BpmGuanlanApprovalClient {

        private BpmAiApprovalSubmitRespDTO submitRespDTO;

        private Config submitConfig;

        @Override
        public BpmAiApprovalSubmitRespDTO submit(cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO reqDTO,
                                                 Config config) {
            this.submitConfig = config;
            return submitRespDTO;
        }

    }

}
