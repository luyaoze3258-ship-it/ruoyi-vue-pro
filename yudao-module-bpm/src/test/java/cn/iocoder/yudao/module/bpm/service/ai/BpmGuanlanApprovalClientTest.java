package cn.iocoder.yudao.module.bpm.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
