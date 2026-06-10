package cn.iocoder.yudao.module.bpm.service.ai;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class BpmAiApprovalCallbackVerifierTest extends BaseMockitoUnitTest {

    private final BpmAiApprovalCallbackVerifier verifier = new BpmAiApprovalCallbackVerifier();

    @Test
    public void verify_validSignature() {
        byte[] body = "{\"task_id\":\"t1\",\"verdict\":\"green\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.sign(body, "secret");

        assertTrue(verifier.verify(body, "secret", signature));
    }

    @Test
    public void verify_tamperedBodyRejected() {
        byte[] body = "{\"task_id\":\"t1\",\"verdict\":\"green\"}".getBytes(StandardCharsets.UTF_8);
        String signature = verifier.sign(body, "secret");
        byte[] tampered = "{\"task_id\":\"t1\",\"verdict\":\"red\"}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(tampered, "secret", signature));
    }

}
