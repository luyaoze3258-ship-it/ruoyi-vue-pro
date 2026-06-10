package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.module.bpm.service.ai.dto.BpmAiApprovalCallbackReqDTO;
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

}
