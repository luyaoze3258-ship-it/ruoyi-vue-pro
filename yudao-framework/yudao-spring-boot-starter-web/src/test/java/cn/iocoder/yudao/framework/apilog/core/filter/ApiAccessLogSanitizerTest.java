package cn.iocoder.yudao.framework.apilog.core.filter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiAccessLogSanitizerTest {

    @Test
    void sanitizeJson_shouldRemoveNestedAgentSecrets() {
        String requestBody = "{\"name\":\"test\",\"apiKey\":\"sk-real\","
                + "\"child\":{\"baseUrl\":\"http://guanlan.guixucloud.cn\",\"clientSecret\":\"secret-real\"},"
                + "\"list\":[{\"refreshToken\":\"token-real\"}]}";

        String result = ApiAccessLogSanitizer.sanitizeJson(requestBody, null);

        assertFalse(result.contains("sk-real"));
        assertFalse(result.contains("secret-real"));
        assertFalse(result.contains("token-real"));
        assertTrue(result.contains("baseUrl"));
    }

    @Test
    void sanitizeJson_shouldRemoveSecretsEmbeddedInTextFields() {
        String requestBody = "{\"bpmnXml\":\"<flowable:aiApprovalSetting><![CDATA["
                + "{\\\"baseUrl\\\":\\\"http://guanlan.guixucloud.cn\\\","
                + "\\\"apiKey\\\":\\\"sk-9c3d2ee00c0f42efbaf1992054dcfb7b\\\"}"
                + "]]></flowable:aiApprovalSetting>\","
                + "\"notes\":\"agentApiKey=sk-embedded clientSecret=secret-real\"}";

        String result = ApiAccessLogSanitizer.sanitizeJson(requestBody, null);

        assertFalse(result.contains("sk-9c3d2ee00c0f42efbaf1992054dcfb7b"));
        assertFalse(result.contains("sk-embedded"));
        assertFalse(result.contains("secret-real"));
        assertTrue(result.contains("http://guanlan.guixucloud.cn"));
        assertTrue(result.contains("<hidden>"));
    }

    @Test
    void sanitizeMap_shouldRemoveSensitiveKeysCaseInsensitive() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("AgentApiKey", "sk-real");
        params.put("name", "test");
        params.put("token", "token-real");

        String result = ApiAccessLogSanitizer.sanitizeMap(params, null);

        assertFalse(result.contains("sk-real"));
        assertFalse(result.contains("token-real"));
        assertTrue(result.contains("test"));
    }

}
