package cn.iocoder.yudao.module.bpm.framework.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

/**
 * BPM AI 审批配置。
 */
@ConfigurationProperties(prefix = "yudao.bpm.ai-approval")
@Validated
@Data
public class BpmAiApprovalProperties {

    /**
     * 是否启用 AI 审批外部提交。
     */
    private Boolean enabled = false;

    /**
     * 观澜接口配置。
     */
    @Valid
    private Guanlan guanlan = new Guanlan();

    @Data
    public static class Guanlan {

        private String baseUrl;

        private String apiKey;

        @Valid
        private Callback callback = new Callback();

    }

    @Data
    public static class Callback {

        private String hmacSecret;

        private String signatureHeader = "X-Approval-Signature";

        private String callbackIdHeader = "X-Callback-Id";

        private String testModeHeader = "X-Approval-Test-Mode";

    }

}
