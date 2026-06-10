package cn.iocoder.yudao.module.bpm.service.ai;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskApproveReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRejectReqVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalCallbackLogDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.task.BpmAiApprovalTaskDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmAiApprovalCallbackLogMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.task.BpmAiApprovalTaskMapper;
import cn.iocoder.yudao.module.bpm.enums.task.BpmAiApprovalStatusEnum;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalBusinessResultReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalSubmitRespDTO;
import cn.iocoder.yudao.module.bpm.service.task.BpmTaskService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * BPM AI 审批 Service 实现。
 */
@Slf4j
@Service
public class BpmAiApprovalServiceImpl implements BpmAiApprovalService {

    private static final String FINAL_APPROVED = "approved";
    private static final String FINAL_REJECTED = "rejected";

    @Resource
    private BpmAiApprovalTaskMapper aiApprovalTaskMapper;
    @Resource
    private BpmAiApprovalCallbackLogMapper callbackLogMapper;
    @Resource
    private BpmGuanlanApprovalClient guanlanApprovalClient;
    @Resource
    private BpmAiApprovalProperties properties;
    @Resource
    private BpmAiApprovalDecisionPolicy decisionPolicy;
    @Resource
    @Lazy
    private BpmTaskService taskService;
    @Resource
    private org.flowable.engine.TaskService flowableTaskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTaskIfEnabled(ProcessInstance processInstance, Task task, FlowElement userTaskElement) {
        BpmSimpleModelNodeVO.AiApprovalSetting setting = BpmnModelUtils.parseAiApprovalSetting(userTaskElement);
        if (setting == null || !BooleanUtil.isTrue(setting.getEnable())) {
            return;
        }
        if (aiApprovalTaskMapper.selectByTaskId(task.getId()) != null) {
            return;
        }
        String externalId = buildExternalId(task);
        BpmAiApprovalTaskDO approvalTask = BpmAiApprovalTaskDO.builder()
                .processInstanceId(processInstance.getId())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .taskId(task.getId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .taskName(task.getName())
                .assigneeUserId(parseAssignee(task))
                .externalId(externalId)
                .enabled(true)
                .adoptEnabled(BooleanUtil.isTrue(setting.getAdoptResult()))
                .status(BpmAiApprovalStatusEnum.SUBMITTED.getStatus())
                .build();
        aiApprovalTaskMapper.insert(approvalTask);

        if (!BooleanUtil.isTrue(properties.getEnabled())) {
            return;
        }
        try {
            BpmAiApprovalSubmitRespDTO submitRespDTO = guanlanApprovalClient.submit(buildSubmitReqDTO(processInstance,
                    task, externalId));
            BpmAiApprovalTaskDO updateObj = new BpmAiApprovalTaskDO();
            updateObj.setId(approvalTask.getId());
            updateObj.setGuanlanTaskId(submitRespDTO.getTaskId());
            aiApprovalTaskMapper.updateById(updateObj);
        } catch (Exception ex) {
            log.error("[submitTaskIfEnabled][taskId({}) 提交观澜失败]", task.getId(), ex);
            BpmAiApprovalTaskDO updateObj = new BpmAiApprovalTaskDO();
            updateObj.setId(approvalTask.getId());
            updateObj.setStatus(BpmAiApprovalStatusEnum.SUBMIT_FAILED.getStatus());
            updateObj.setSubmitError(StrUtil.subPre(ex.getMessage(), 500));
            aiApprovalTaskMapper.updateById(updateObj);
        }
    }

    @Override
    @TenantIgnore
    @Transactional(rollbackFor = Exception.class)
    public boolean handleCallback(BpmAiApprovalCallbackReqDTO callbackReqDTO, String callbackId, String rawBody,
                                  boolean testMode, boolean verified) {
        if (StrUtil.isBlank(callbackId)) {
            callbackId = callbackReqDTO.getCallbackId();
        }
        if (StrUtil.isBlank(callbackId)) {
            throw new IllegalArgumentException("callbackId is blank");
        }
        try {
            callbackLogMapper.insert(BpmAiApprovalCallbackLogDO.builder()
                    .callbackId(callbackId)
                    .guanlanTaskId(callbackReqDTO.getTaskId())
                    .externalId(callbackReqDTO.getExternalId())
                    .verdict(callbackReqDTO.getVerdict())
                    .testMode(testMode)
                    .verified(verified)
                    .processed(false)
                    .rawBody(rawBody)
                    .receivedTime(LocalDateTime.now())
                    .build());
        } catch (DuplicateKeyException duplicateKeyException) {
            return false;
        }
        if (testMode) {
            markCallbackProcessed(callbackId, true, null);
            return true;
        }

        BpmAiApprovalTaskDO approvalTask = selectApprovalTaskForUpdate(callbackReqDTO);
        if (approvalTask == null) {
            markCallbackProcessed(callbackId, false, "AI approval task not found");
            throw new IllegalArgumentException("AI approval task not found");
        }
        updateCallbackResult(approvalTask, callbackReqDTO, callbackId);
        applyDecision(approvalTask, callbackReqDTO);
        markCallbackProcessed(callbackId, true, null);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncBusinessResultIfNecessary(String taskId, String finalVerdict, String finalOpinion) {
        BpmAiApprovalTaskDO approvalTask = aiApprovalTaskMapper.selectByTaskIdForUpdate(taskId);
        if (approvalTask == null || StrUtil.isBlank(approvalTask.getGuanlanTaskId())
                || StrUtil.isNotBlank(approvalTask.getBusinessFinalVerdict())) {
            return;
        }
        if (BooleanUtil.isTrue(properties.getEnabled())) {
            BpmAiApprovalBusinessResultReqDTO reqDTO = new BpmAiApprovalBusinessResultReqDTO()
                    .setFinalVerdict(finalVerdict)
                    .setFinalOpinion(finalOpinion)
                    .setProcessInstanceId(approvalTask.getProcessInstanceId())
                    .setTaskId(taskId);
            guanlanApprovalClient.submitBusinessResult(approvalTask.getGuanlanTaskId(), reqDTO);
        }
        BpmAiApprovalTaskDO updateObj = new BpmAiApprovalTaskDO();
        updateObj.setId(approvalTask.getId());
        updateObj.setBusinessFinalVerdict(finalVerdict);
        updateObj.setBusinessFinalOpinion(finalOpinion);
        updateObj.setBusinessResultSyncTime(LocalDateTime.now());
        updateObj.setStatus(BpmAiApprovalStatusEnum.BUSINESS_RESULT_SYNCED.getStatus());
        aiApprovalTaskMapper.updateById(updateObj);
    }

    private BpmAiApprovalSubmitReqDTO buildSubmitReqDTO(ProcessInstance processInstance, Task task, String externalId) {
        Map<String, Object> document = new HashMap<>();
        document.put("processInstanceId", processInstance.getId());
        document.put("processDefinitionId", processInstance.getProcessDefinitionId());
        document.put("businessKey", processInstance.getBusinessKey());
        document.put("processVariables", processInstance.getProcessVariables());
        document.put("taskId", task.getId());
        document.put("taskDefinitionKey", task.getTaskDefinitionKey());
        document.put("taskName", task.getName());
        document.put("assigneeUserId", parseAssignee(task));

        return new BpmAiApprovalSubmitReqDTO()
                .setExternalId(externalId)
                .setProcessInstanceId(processInstance.getId())
                .setProcessDefinitionId(processInstance.getProcessDefinitionId())
                .setTaskId(task.getId())
                .setTaskDefinitionKey(task.getTaskDefinitionKey())
                .setTaskName(task.getName())
                .setAssigneeUserId(parseAssignee(task))
                .setDocument(document);
    }

    private BpmAiApprovalTaskDO selectApprovalTaskForUpdate(BpmAiApprovalCallbackReqDTO callbackReqDTO) {
        if (StrUtil.isNotBlank(callbackReqDTO.getTaskId())) {
            BpmAiApprovalTaskDO approvalTask = aiApprovalTaskMapper.selectByGuanlanTaskIdForUpdate(callbackReqDTO.getTaskId());
            if (approvalTask != null) {
                return approvalTask;
            }
        }
        if (StrUtil.isNotBlank(callbackReqDTO.getExternalId())) {
            return aiApprovalTaskMapper.selectByExternalId(callbackReqDTO.getExternalId());
        }
        return null;
    }

    private void updateCallbackResult(BpmAiApprovalTaskDO approvalTask, BpmAiApprovalCallbackReqDTO callbackReqDTO,
                                      String callbackId) {
        BpmAiApprovalTaskDO updateObj = new BpmAiApprovalTaskDO();
        updateObj.setId(approvalTask.getId());
        updateObj.setGuanlanTaskId(StrUtil.blankToDefault(approvalTask.getGuanlanTaskId(), callbackReqDTO.getTaskId()));
        updateObj.setCallbackId(callbackId);
        updateObj.setCallbackTime(LocalDateTime.now());
        updateObj.setVerdict(callbackReqDTO.getVerdict());
        updateObj.setOpinion(callbackReqDTO.getOpinion());
        updateObj.setStatus(BpmAiApprovalStatusEnum.CALLBACK_RECEIVED.getStatus());
        aiApprovalTaskMapper.updateById(updateObj);
        approvalTask.setVerdict(callbackReqDTO.getVerdict());
        approvalTask.setOpinion(callbackReqDTO.getOpinion());
    }

    void applyDecision(BpmAiApprovalTaskDO approvalTask, BpmAiApprovalCallbackReqDTO callbackReqDTO) {
        BpmAiApprovalDecisionPolicy.Decision decision = decisionPolicy.decide(BooleanUtil.isTrue(approvalTask.getAdoptEnabled()),
                callbackReqDTO.getVerdict());
        if (decision.getAction() == BpmAiApprovalDecisionPolicy.Action.APPROVE) {
            taskService.approveTask(getCurrentAssigneeUserId(approvalTask.getTaskId()),
                    new BpmTaskApproveReqVO().setId(approvalTask.getTaskId()).setReason(buildAiReason(callbackReqDTO)));
            markApprovalTaskStatus(approvalTask.getId(), BpmAiApprovalStatusEnum.AUTO_APPROVED);
            return;
        }
        if (decision.getAction() == BpmAiApprovalDecisionPolicy.Action.REJECT) {
            taskService.rejectTask(getCurrentAssigneeUserId(approvalTask.getTaskId()),
                    new BpmTaskRejectReqVO().setId(approvalTask.getTaskId()).setReason(buildAiReason(callbackReqDTO)));
            markApprovalTaskStatus(approvalTask.getId(), BpmAiApprovalStatusEnum.AUTO_REJECTED);
            return;
        }
        flowableTaskService.setVariableLocal(approvalTask.getTaskId(), BpmnVariableConstants.TASK_VARIABLE_REASON,
                buildAiReason(callbackReqDTO));
        markApprovalTaskStatus(approvalTask.getId(), BpmAiApprovalStatusEnum.SUGGESTED);
    }

    private void markApprovalTaskStatus(Long id, BpmAiApprovalStatusEnum statusEnum) {
        BpmAiApprovalTaskDO updateObj = new BpmAiApprovalTaskDO();
        updateObj.setId(id);
        updateObj.setStatus(statusEnum.getStatus());
        aiApprovalTaskMapper.updateById(updateObj);
    }

    private void markCallbackProcessed(String callbackId, boolean processed, String errorMessage) {
        BpmAiApprovalCallbackLogDO logDO = callbackLogMapper.selectByCallbackId(callbackId);
        if (logDO == null) {
            return;
        }
        BpmAiApprovalCallbackLogDO updateObj = new BpmAiApprovalCallbackLogDO();
        updateObj.setId(logDO.getId());
        updateObj.setProcessed(processed);
        updateObj.setErrorMessage(errorMessage);
        callbackLogMapper.updateById(updateObj);
    }

    private static String buildExternalId(Task task) {
        return task.getProcessInstanceId() + ":" + task.getId();
    }

    private static Long parseAssignee(Task task) {
        return StrUtil.isBlank(task.getAssignee()) ? null : Long.valueOf(task.getAssignee());
    }

    private Long getCurrentAssigneeUserId(String taskId) {
        Task currentTask = taskService.getTask(taskId);
        return currentTask == null ? null : parseAssignee(currentTask);
    }

    private static String buildAiReason(BpmAiApprovalCallbackReqDTO callbackReqDTO) {
        String opinion = StrUtil.blankToDefault(callbackReqDTO.getOpinion(), "");
        return StrUtil.isBlank(opinion) ? "AI 审批结论：" + callbackReqDTO.getVerdict()
                : "AI 审批结论：" + callbackReqDTO.getVerdict() + "；" + opinion;
    }

}
