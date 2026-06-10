package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalChatRespDTO;
import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalDetailRespDTO;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;

public interface BpmAiApprovalService {

    /**
     * 用户任务创建后，按节点配置提交 AI 审批。
     */
    void submitTaskIfEnabled(ProcessInstance processInstance, Task task, FlowElement userTaskElement);

    /**
     * 处理观澜回调。
     */
    boolean handleCallback(BpmAiApprovalCallbackReqDTO callbackReqDTO, String callbackId, String rawBody,
                           boolean testMode, boolean verified);

    /**
     * 人工完成任务后，向观澜回灌业务终态。
     */
    void syncBusinessResultIfNecessary(String taskId, String finalVerdict, String finalOpinion);

    /**
     * 主动查询观澜任务结果，并在任务完成后应用 AI 结论。
     *
     * @param taskId BPM 任务编号
     * @return 是否已同步完成结果
     */
    boolean syncTaskResultFromGuanlan(String taskId);

    /**
     * 批量查询未完成的观澜任务结果，并在任务完成后应用 AI 结论。
     *
     * @param batchSize 每次查询的最大任务数量
     * @return 已同步完成结果的任务数量
     */
    int syncPendingTaskResultsFromGuanlan(int batchSize);

    /**
     * 获得流程实例最近一次 AI 审批详情。
     */
    BpmAiApprovalDetailRespDTO getLatestDetail(String processInstanceId);

    /**
     * 围绕当前单据和 AI 审批结果进行问答。
     */
    BpmAiApprovalChatRespDTO chat(String processInstanceId, String question);

}
