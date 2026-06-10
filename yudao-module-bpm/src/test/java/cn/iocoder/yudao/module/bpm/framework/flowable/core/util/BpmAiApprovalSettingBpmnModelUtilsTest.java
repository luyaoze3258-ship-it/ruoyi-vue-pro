package cn.iocoder.yudao.module.bpm.framework.flowable.core.util;

import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmSimpleModelNodeTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskApproveMethodEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskApproveTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskAssignEmptyHandlerTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskAssignStartUserHandlerTypeEnum;
import cn.iocoder.yudao.module.bpm.enums.definition.BpmUserTaskRejectHandlerTypeEnum;
import cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BpmAiApprovalSettingBpmnModelUtilsTest {

    @Test
    public void buildBpmnModel_shouldKeepAiApprovalSettingAfterXmlRoundTrip() {
        BpmSimpleModelNodeVO.AiApprovalSetting aiApprovalSetting = new BpmSimpleModelNodeVO.AiApprovalSetting()
                .setEnable(true).setAdoptResult(true)
                .setAgentName("观澜费用审核")
                .setBaseUrl("https://guanlan.guixucloud.com/")
                .setApiKey("test-api-key");
        BpmSimpleModelNodeVO node = new BpmSimpleModelNodeVO()
                .setId("Activity_ai")
                .setType(BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType())
                .setName("AI 审批")
                .setApproveType(BpmUserTaskApproveTypeEnum.USER.getType())
                .setCandidateStrategy(BpmTaskCandidateStrategyEnum.START_USER.getStrategy())
                .setApproveMethod(BpmUserTaskApproveMethodEnum.SEQUENTIAL.getMethod())
                .setRejectHandler(new BpmSimpleModelNodeVO.RejectHandler()
                        .setType(BpmUserTaskRejectHandlerTypeEnum.FINISH_PROCESS_INSTANCE.getType()))
                .setAssignEmptyHandler(new BpmSimpleModelNodeVO.AssignEmptyHandler()
                        .setType(BpmUserTaskAssignEmptyHandlerTypeEnum.APPROVE.getType()))
                .setAssignStartUserHandlerType(BpmUserTaskAssignStartUserHandlerTypeEnum.START_USER_AUDIT.getType())
                .setAiApprovalSetting(aiApprovalSetting)
                .setChildNode(new BpmSimpleModelNodeVO()
                        .setId("EndEvent")
                        .setType(BpmSimpleModelNodeTypeEnum.END_NODE.getType())
                        .setName("结束"));

        BpmnModel model = SimpleModelUtils.buildBpmnModel("ai-approval-test", "AI 审批测试", node);
        String xml = BpmnModelUtils.getBpmnXml(model);
        BpmnModel parsedModel = BpmnModelUtils.getBpmnModel(xml.getBytes());
        FlowElement parsedUserTask = BpmnModelUtils.getFlowElementById(parsedModel, "Activity_ai");

        BpmSimpleModelNodeVO.AiApprovalSetting parsedSetting = BpmnModelUtils.parseAiApprovalSetting(parsedUserTask);
        assertNotNull(parsedSetting);
        assertTrue(parsedSetting.getEnable());
        assertTrue(parsedSetting.getAdoptResult());
        assertEquals("观澜费用审核", parsedSetting.getAgentName());
        assertEquals("https://guanlan.guixucloud.com/", parsedSetting.getBaseUrl());
        assertEquals("test-api-key", parsedSetting.getApiKey());
    }

    @Test
    public void parseAiApprovalSetting_shouldSupportBpmnDesignerElementText() {
        BpmSimpleModelNodeVO.AiApprovalSetting aiApprovalSetting = new BpmSimpleModelNodeVO.AiApprovalSetting()
                .setEnable(true).setAdoptResult(false)
                .setAgentName("观澜合同审核")
                .setBaseUrl("https://guanlan.guixucloud.com/")
                .setApiKey("designer-test-api-key");
        org.flowable.bpmn.model.UserTask userTask = new org.flowable.bpmn.model.UserTask();

        BpmnModelUtils.addAiApprovalSetting(aiApprovalSetting, userTask);

        BpmSimpleModelNodeVO.AiApprovalSetting parsedSetting = BpmnModelUtils.parseAiApprovalSetting(userTask);
        assertNotNull(parsedSetting);
        assertTrue(parsedSetting.getEnable());
        assertFalse(parsedSetting.getAdoptResult());
        assertEquals("观澜合同审核", parsedSetting.getAgentName());
        assertEquals("https://guanlan.guixucloud.com/", parsedSetting.getBaseUrl());
        assertEquals("designer-test-api-key", parsedSetting.getApiKey());
    }

}
