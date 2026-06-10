package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.definition.BpmProcessDefinitionInfoDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalTaskDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmAiApprovalTaskMapper;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmModelFormTypeEnum;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitRespDTO;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
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

import java.util.List;
import java.util.Map;

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
    private BpmProcessDefinitionService processDefinitionService;
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
        when(processDefinitionService.getProcessDefinitionInfo("definition-1")).thenReturn(normalProcessDefinitionInfo(
                "{\"type\":\"inputNumber\",\"field\":\"amount\",\"title\":\"金额\"}"));
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
                .setBaseUrl("http://guanlan.guixucloud.cn")
                .setApiKey("node-api-key"), userTask);
        guanlanApprovalClient.submitRespDTO = new BpmAiApprovalSubmitRespDTO()
                .setTaskId("guanlan-task-1").setStatus("accepted");

        aiApprovalService.submitTaskIfEnabled(processInstance, task, userTask);

        verify(aiApprovalTaskMapper).insert(argThat((BpmAiApprovalTaskDO approvalTask) ->
                "观澜费用审核".equals(approvalTask.getGuanlanAgentName())
                        && "http://guanlan.guixucloud.cn".equals(approvalTask.getGuanlanBaseUrl())
                        && "node-api-key".equals(approvalTask.getGuanlanApiKey())));
        org.junit.jupiter.api.Assertions.assertEquals("http://guanlan.guixucloud.cn",
                guanlanApprovalClient.submitConfig.getBaseUrl());
        org.junit.jupiter.api.Assertions.assertEquals("node-api-key", guanlanApprovalClient.submitConfig.getApiKey());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void submitTaskIfEnabled_shouldSubmitOnlyDeclaredFormFields() {
        ProcessInstance processInstance = mock(ProcessInstance.class);
        when(processInstance.getId()).thenReturn("process-1");
        when(processInstance.getProcessDefinitionId()).thenReturn("definition-1");
        when(processInstance.getBusinessKey()).thenReturn("biz-1");
        when(processInstance.getProcessVariables()).thenReturn(Map.of(
                "subject", "合同付款审批",
                "reason", "供应商尾款支付",
                "external_id", "EXP-2026-00193",
                "amount", 1280.5,
                BpmnVariableConstants.PROCESS_INSTANCE_VARIABLE_START_USER_ID, 1L));
        when(processDefinitionService.getProcessDefinitionInfo("definition-1")).thenReturn(normalProcessDefinitionInfo(
                "{\"type\":\"input\",\"field\":\"subject\",\"title\":\"申请主题\"}",
                "{\"type\":\"input\",\"field\":\"reason\",\"title\":\"申请说明\"}"));
        Task task = mock(Task.class);
        when(task.getId()).thenReturn("task-1");
        when(task.getTaskDefinitionKey()).thenReturn("Activity_ai");
        when(task.getName()).thenReturn("AI 审批节点");
        when(task.getAssignee()).thenReturn("1");
        when(task.getProcessInstanceId()).thenReturn("process-1");
        UserTask userTask = new UserTask();
        BpmnModelUtils.addAiApprovalSetting(new BpmSimpleModelNodeVO.AiApprovalSetting()
                .setEnable(true)
                .setBaseUrl("http://guanlan.guixucloud.cn")
                .setApiKey("node-api-key"), userTask);
        guanlanApprovalClient.submitRespDTO = new BpmAiApprovalSubmitRespDTO()
                .setTaskId("guanlan-task-1").setStatus("accepted");

        aiApprovalService.submitTaskIfEnabled(processInstance, task, userTask);

        Map<String, Object> document = guanlanApprovalClient.submitReqDTO.getDocument();
        Map<String, Object> formVariables = (Map<String, Object>) document.get("formVariables");
        org.junit.jupiter.api.Assertions.assertEquals(Map.of(
                "subject", "合同付款审批",
                "reason", "供应商尾款支付"), formVariables);
        org.junit.jupiter.api.Assertions.assertFalse(document.containsKey("processVariables"));
        org.junit.jupiter.api.Assertions.assertFalse(formVariables.containsKey("external_id"));
        org.junit.jupiter.api.Assertions.assertFalse(formVariables.containsKey("amount"));
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

    @Test
    public void syncTaskResultFromGuanlan_shouldApplyCompletedResult() {
        BpmAiApprovalTaskDO approvalTask = BpmAiApprovalTaskDO.builder()
                .id(10L)
                .taskId("task-1")
                .externalId("external-1")
                .guanlanTaskId("guanlan-task-1")
                .guanlanBaseUrl("http://guanlan.guixucloud.cn")
                .guanlanApiKey("node-api-key")
                .adoptEnabled(false)
                .build();
        when(aiApprovalTaskMapper.selectByTaskIdForUpdate("task-1")).thenReturn(approvalTask);
        guanlanApprovalClient.taskResult = new BpmGuanlanApprovalClient.TaskResult()
                .setTaskId("guanlan-task-1")
                .setExternalId("external-1")
                .setStatus("completed")
                .setVerdict("yellow")
                .setOpinion("需人工复核");

        boolean synced = aiApprovalService.syncTaskResultFromGuanlan("task-1");

        org.junit.jupiter.api.Assertions.assertTrue(synced);
        verify(aiApprovalTaskMapper).updateById(argThat((BpmAiApprovalTaskDO updateObj) ->
                Long.valueOf(10L).equals(updateObj.getId())
                        && "poll:guanlan-task-1".equals(updateObj.getCallbackId())
                        && "yellow".equals(updateObj.getVerdict())
                        && "需人工复核".equals(updateObj.getOpinion())));
        verify(flowableTaskService).setVariableLocal(eq("task-1"), eq(BpmnVariableConstants.TASK_VARIABLE_REASON),
                eq("AI 审批结论：yellow；需人工复核"));
    }

    @Test
    public void syncPendingTaskResultsFromGuanlan_shouldPollPendingTasksAndContinueOnFailure() {
        BpmAiApprovalTaskDO completedTask = BpmAiApprovalTaskDO.builder()
                .id(10L)
                .taskId("task-completed")
                .externalId("external-completed")
                .guanlanTaskId("guanlan-completed")
                .guanlanBaseUrl("http://guanlan.guixucloud.cn")
                .guanlanApiKey("node-api-key")
                .adoptEnabled(false)
                .build();
        BpmAiApprovalTaskDO pendingTask = BpmAiApprovalTaskDO.builder()
                .id(11L)
                .taskId("task-pending")
                .externalId("external-pending")
                .guanlanTaskId("guanlan-pending")
                .guanlanBaseUrl("http://guanlan.guixucloud.cn")
                .guanlanApiKey("node-api-key")
                .adoptEnabled(false)
                .build();
        BpmAiApprovalTaskDO failedTask = BpmAiApprovalTaskDO.builder()
                .id(12L)
                .taskId("task-failed")
                .externalId("external-failed")
                .guanlanTaskId("guanlan-failed")
                .guanlanBaseUrl("http://guanlan.guixucloud.cn")
                .guanlanApiKey("node-api-key")
                .adoptEnabled(false)
                .build();
        when(aiApprovalTaskMapper.selectPendingPollingTasks(3))
                .thenReturn(List.of(completedTask, pendingTask, failedTask));
        when(aiApprovalTaskMapper.selectByTaskIdForUpdate("task-completed")).thenReturn(completedTask);
        when(aiApprovalTaskMapper.selectByTaskIdForUpdate("task-pending")).thenReturn(pendingTask);
        when(aiApprovalTaskMapper.selectByTaskIdForUpdate("task-failed")).thenReturn(failedTask);
        guanlanApprovalClient.taskResults.put("guanlan-completed", new BpmGuanlanApprovalClient.TaskResult()
                .setTaskId("guanlan-completed")
                .setExternalId("external-completed")
                .setStatus("completed")
                .setVerdict("green")
                .setOpinion("材料完整"));
        guanlanApprovalClient.taskResults.put("guanlan-pending", new BpmGuanlanApprovalClient.TaskResult()
                .setTaskId("guanlan-pending")
                .setExternalId("external-pending")
                .setStatus("processing"));
        guanlanApprovalClient.failedTaskIds.add("guanlan-failed");

        int syncedCount = aiApprovalService.syncPendingTaskResultsFromGuanlan(3);

        org.junit.jupiter.api.Assertions.assertEquals(1, syncedCount);
        verify(aiApprovalTaskMapper).updateById(argThat((BpmAiApprovalTaskDO updateObj) ->
                Long.valueOf(10L).equals(updateObj.getId())
                        && "poll:guanlan-completed".equals(updateObj.getCallbackId())
                        && "green".equals(updateObj.getVerdict())));
        verify(flowableTaskService).setVariableLocal(eq("task-completed"), eq(BpmnVariableConstants.TASK_VARIABLE_REASON),
                eq("AI 审批结论：green；材料完整"));
        verify(aiApprovalTaskMapper, never()).updateById(argThat((BpmAiApprovalTaskDO updateObj) ->
                Long.valueOf(11L).equals(updateObj.getId()) || Long.valueOf(12L).equals(updateObj.getId())));
    }

    private static class FakeGuanlanApprovalClient extends BpmGuanlanApprovalClient {

        private BpmAiApprovalSubmitRespDTO submitRespDTO;

        private cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO submitReqDTO;

        private Config submitConfig;

        private TaskResult taskResult;

        private final java.util.Map<String, TaskResult> taskResults = new java.util.HashMap<>();

        private final java.util.Set<String> failedTaskIds = new java.util.HashSet<>();

        @Override
        public BpmAiApprovalSubmitRespDTO submit(cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO reqDTO,
                                                 Config config) {
            this.submitReqDTO = reqDTO;
            this.submitConfig = config;
            return submitRespDTO;
        }

        @Override
        public TaskResult getTask(String taskId, Config config) {
            this.submitConfig = config;
            if (failedTaskIds.contains(taskId)) {
                throw new IllegalStateException("query failed");
            }
            if (taskResults.containsKey(taskId)) {
                return taskResults.get(taskId);
            }
            return taskResult;
        }

    }

    private static BpmProcessDefinitionInfoDO normalProcessDefinitionInfo(String... formFields) {
        return BpmProcessDefinitionInfoDO.builder()
                .formType(BpmModelFormTypeEnum.NORMAL.getType())
                .formFields(List.of(formFields))
                .build();
    }

}
