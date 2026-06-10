package cn.iocoder.yudao.framework.apilog.core.filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * API 访问日志脱敏工具。
 */
@Slf4j
public class ApiAccessLogSanitizer {

    private static final String[] SANITIZE_KEYS = new String[]{
            "password", "token", "accessToken", "refreshToken",
            "apiKey", "apikey", "agentApiKey", "secret", "clientSecret", "hmacSecret"
    };

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
            for (JsonNode childNode : node) {
                sanitizeJson(childNode, sanitizeKeys);
            }
            return;
        }
        // 情况二：非 Object，只是某个值，直接返回
        if (!node.isObject()) {
            return;
        }
        //  情况三：Object，遍历处理
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isSensitiveKey(entry.getKey(), sanitizeKeys)) {
                iterator.remove();
                continue;
            }
            sanitizeJson(entry.getValue(), sanitizeKeys);
        }
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
