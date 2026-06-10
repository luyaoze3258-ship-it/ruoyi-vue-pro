package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.bpm.framework.ai.config.BpmAiApprovalProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

public class BpmAiApprovalPollingJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmAiApprovalPollingJob pollingJob;

    @Mock
    private BpmAiApprovalService aiApprovalService;

    private BpmAiApprovalProperties properties;

    @BeforeEach
    public void setUp() {
        properties = new BpmAiApprovalProperties();
        properties.setEnabled(true);
        properties.getPolling().setEnabled(true);
        properties.getPolling().setBatchSize(20);
        ReflectionTestUtils.setField(pollingJob, "properties", properties);
    }

    @Test
    public void pollGuanlanResults_shouldUseConfiguredBatchSize() {
        pollingJob.pollGuanlanResults();

        verify(aiApprovalService).syncPendingTaskResultsFromGuanlan(20);
    }

    @Test
    public void pollGuanlanResults_shouldSkipWhenDisabled() {
        properties.getPolling().setEnabled(false);

        pollingJob.pollGuanlanResults();

        verify(aiApprovalService, never()).syncPendingTaskResultsFromGuanlan(anyInt());
    }

}
