package cn.iocoder.yudao.module.bpm.service.ai;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 观澜回调 HMAC 验签。
 */
@Component
public class BpmAiApprovalCallbackVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public boolean verify(byte[] rawBody, String secret, String signature) {
        if (StrUtil.isBlank(secret) || StrUtil.isBlank(signature)) {
            return false;
        }
        String expected = sign(rawBody, secret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.trim().getBytes(StandardCharsets.UTF_8));
    }

    public String sign(byte[] rawBody, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(rawBody);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Sign Guanlan callback failed", e);
        }
    }

}
