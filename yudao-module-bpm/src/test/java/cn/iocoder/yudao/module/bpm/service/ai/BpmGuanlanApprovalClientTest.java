package cn.iocoder.yudao.module.bpm.service.ai;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpmGuanlanApprovalClientTest {

    private final BpmGuanlanApprovalClient client = new BpmGuanlanApprovalClient();

    @Test
    void parseSubmitTaskId_shouldSupportTopLevelTaskId() {
        assertEquals("task-1", client.parseSubmitTaskId("{\"task_id\":\"task-1\"}"));
        assertEquals("task-2", client.parseSubmitTaskId("{\"taskId\":\"task-2\"}"));
        assertEquals("task-3", client.parseSubmitTaskId("{\"id\":\"task-3\"}"));
    }

    @Test
    void parseSubmitTaskId_shouldSupportDataWrappedTaskId() {
        assertEquals("task-4", client.parseSubmitTaskId("{\"code\":0,\"data\":{\"task_id\":\"task-4\"}}"));
        assertEquals("task-5", client.parseSubmitTaskId("{\"data\":{\"taskId\":\"task-5\"}}"));
        assertEquals("task-6", client.parseSubmitTaskId("{\"data\":{\"id\":\"task-6\"}}"));
    }

    @Test
    void parseTaskResult_shouldSupportGuanlanTaskResponse() {
        BpmGuanlanApprovalClient.TaskResult taskResult = client.parseTaskResult("{\"task_id\":\"task-1\","
                + "\"external_id\":\"external-1\",\"status\":\"completed\",\"verdict\":\"yellow\","
                + "\"opinion\":\"需要人工复核\"}");

        assertEquals("task-1", taskResult.getTaskId());
        assertEquals("external-1", taskResult.getExternalId());
        assertEquals("completed", taskResult.getStatus());
        assertEquals("yellow", taskResult.getVerdict());
        assertEquals("需要人工复核", taskResult.getOpinion());
    }

    @Test
    void buildChatHeaders_shouldUseDoubleSubmitCsrf() {
        HttpHeaders headers = client.buildChatHeadersForTest();

        String csrfToken = headers.getFirst("X-CSRF-Token");
        assertTrue(csrfToken != null && !csrfToken.isBlank());
        assertEquals("csrf_token=" + csrfToken, headers.getFirst(HttpHeaders.COOKIE));
    }

    @Test
    void parseChatAnswer_shouldReadAssistantMessagesFromGuanlanRunResponse() {
        String answer = client.parseChatAnswer("{\"messages\":["
                + "{\"type\":\"human\",\"content\":\"问题\"},"
                + "{\"type\":\"ai\",\"content\":\"这是观澜回答\"}"
                + "]}");

        assertEquals("这是观澜回答", answer);
    }

    @Test
    void buildChatPrompt_shouldEmbedCurrentDocumentContext() {
        String prompt = client.buildChatPrompt("为什么需要人工复核？", java.util.Map.of(
                "processInstanceId", "process-1",
                "taskName", "AI 审批节点",
                "guanlanTaskId", "guanlan-task-1",
                "agentName", "费用审核智能体",
                "conclusion", "AI结论：需人工复核",
                "formVariables", java.util.Map.of("subject", "差旅报销", "reason", "金额不一致"),
                "opinion", "总金额与明细金额不一致，需要人工复核。"));

        assertTrue(prompt.contains("为什么需要人工复核？"));
        assertTrue(prompt.contains("process-1"));
        assertTrue(prompt.contains("AI结论：需人工复核"));
        assertTrue(prompt.contains("subject=差旅报销"));
        assertTrue(prompt.contains("总金额与明细金额不一致，需要人工复核。"));
        assertTrue(prompt.contains("不要要求用户重新上传单据"));
    }

}
