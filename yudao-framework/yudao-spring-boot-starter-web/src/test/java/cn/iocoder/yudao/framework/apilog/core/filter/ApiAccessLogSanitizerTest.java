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
                + "\"child\":{\"baseUrl\":\"https://guanlan.guixucloud.com/\",\"clientSecret\":\"secret-real\"},"
                + "\"list\":[{\"refreshToken\":\"token-real\"}]}";

        String result = ApiAccessLogSanitizer.sanitizeJson(requestBody, null);

        assertFalse(result.contains("sk-real"));
        assertFalse(result.contains("secret-real"));
        assertFalse(result.contains("token-real"));
        assertTrue(result.contains("baseUrl"));
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
