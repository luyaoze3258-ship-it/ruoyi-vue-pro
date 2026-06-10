package cn.iocoder.yudao.framework.apilog.core.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API 访问日志脱敏工具。
 */
@Slf4j
public class ApiAccessLogSanitizer {

    private static final String[] SANITIZE_KEYS = new String[]{
            "password", "token", "accessToken", "refreshToken",
            "apiKey", "apikey", "agentApiKey", "secret", "clientSecret", "hmacSecret"
    };
    private static final String SENSITIVE_KEY_PATTERN = "password|token|accessToken|refreshToken|apiKey|apikey"
            + "|agentApiKey|secret|clientSecret|hmacSecret";
    private static final Pattern JSON_TEXT_SECRET_PATTERN = Pattern.compile(
            "(?i)(\\\"(?:" + SENSITIVE_KEY_PATTERN + ")\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern KEY_VALUE_TEXT_SECRET_PATTERN = Pattern.compile(
            "(?i)((?:" + SENSITIVE_KEY_PATTERN + ")\\s*[=:]\\s*)[^\\s,&;<>\\\"]+");
    private static final Pattern OPENAI_STYLE_KEY_PATTERN = Pattern.compile("sk-[A-Za-z0-9_-]{12,}");

    public static String sanitizeMap(Map<String, ?> map, String[] sanitizeKeys) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }
        Map<String, Object> sanitizedMap = MapUtil.newHashMap(map.size());
        map.forEach((key, value) -> {
            if (!isSensitiveKey(key, sanitizeKeys)) {
                sanitizedMap.put(key, value);
            }
        });
        return JsonUtils.toJsonString(sanitizedMap);
    }

    public static String sanitizeJson(String jsonString, String[] sanitizeKeys) {
        if (StrUtil.isEmpty(jsonString)) {
            return null;
        }
        try {
            JsonNode rootNode = JsonUtils.parseTree(jsonString);
            sanitizeJson(rootNode, sanitizeKeys);
            return JsonUtils.toJsonString(rootNode);
        } catch (Exception e) {
            // 脱敏失败时不返回原始内容，避免把密钥写入日志。
            log.warn("[sanitizeJson][脱敏失败，已隐藏请求体]", e);
            return "<hidden>";
        }
    }

    public static void sanitizeJson(JsonNode node, String[] sanitizeKeys) {
        if (node == null) {
            return;
        }
        // 情况一：数组，遍历处理
        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode childNode = arrayNode.get(i);
                if (childNode.isTextual()) {
                    arrayNode.set(i, TextNode.valueOf(sanitizeText(childNode.asText())));
                    continue;
                }
                sanitizeJson(childNode, sanitizeKeys);
            }
            return;
        }
        // 情况二：非 Object，只是某个值，直接返回
        if (!node.isObject()) {
            return;
        }
        //  情况三：Object，遍历处理
        ObjectNode objectNode = (ObjectNode) node;
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isSensitiveKey(entry.getKey(), sanitizeKeys)) {
                iterator.remove();
                continue;
            }
            if (entry.getValue().isTextual()) {
                objectNode.put(entry.getKey(), sanitizeText(entry.getValue().asText()));
                continue;
            }
            sanitizeJson(entry.getValue(), sanitizeKeys);
        }
    }

    private static String sanitizeText(String text) {
        if (StrUtil.isBlank(text)) {
            return text;
        }
        String sanitized = JSON_TEXT_SECRET_PATTERN.matcher(text).replaceAll("$1<hidden>$2");
        sanitized = KEY_VALUE_TEXT_SECRET_PATTERN.matcher(sanitized).replaceAll("$1<hidden>");
        return OPENAI_STYLE_KEY_PATTERN.matcher(sanitized).replaceAll("<hidden>");
    }

    private static boolean isSensitiveKey(String key, String[] sanitizeKeys) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        if (ArrayUtil.containsIgnoreCase(SANITIZE_KEYS, key)
                || ArrayUtil.containsIgnoreCase(sanitizeKeys, key)) {
            return true;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return lowerKey.contains("password")
                || lowerKey.contains("token")
                || lowerKey.contains("apikey")
                || lowerKey.contains("api-key")
                || lowerKey.contains("secret");
    }

}
