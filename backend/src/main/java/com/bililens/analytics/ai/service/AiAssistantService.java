package com.bililens.analytics.ai.service;

import com.bililens.analytics.ai.dto.AiChatRequest;
import com.bililens.analytics.ai.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultUser;

    public AiAssistantService(
            @Value("${analytics.dify.base-url:}") String baseUrl,
            @Value("${analytics.dify.api-key:}") String apiKey,
            @Value("${analytics.dify.user:bililens-user}") String defaultUser
    ) {
        this.restClient = RestClient.create();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.defaultUser = defaultUser;
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return new AiChatResponse("请输入要咨询的问题。", request.conversationId(), hasDifyConfig());
        }

        if (!hasDifyConfig()) {
            return new AiChatResponse(buildLocalAnswer(request), null, false);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", Map.of());
        body.put("query", buildPrompt(request));
        body.put("response_mode", "blocking");
        body.put("user", request.user() == null || request.user().isBlank() ? defaultUser : request.user());
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            body.put("conversation_id", request.conversationId());
        }

        Map<?, ?> response = restClient.post()
                .uri(baseUrl + "/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        String answer = valueAsString(response, "answer", "Dify 未返回 answer 字段。");
        String conversationId = valueAsString(response, "conversation_id", request.conversationId());
        return new AiChatResponse(answer, conversationId, true);
    }

    private boolean hasDifyConfig() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    private String buildPrompt(AiChatRequest request) {
        return """
                你是 BiliLens 数据可视化平台的 AI 数据分析助手。
                请基于用户问题和页面上下文回答，优先给出数据解释、原因判断和可执行建议。
                如果上下文不足，请明确说明缺少哪些数据，不要编造指标。

                功能场景：%s
                用户问题：%s

                页面上下文：
                %s
                """.formatted(normalizeMode(request.mode()), request.query(), compactContext(request.context()));
    }

    private String buildLocalAnswer(AiChatRequest request) {
        return """
                Dify 尚未配置，当前返回本地分析提示。

                已识别场景：%s
                你的问题：%s

                当前页面上下文摘要：
                %s

                要启用真正的 AI 分析，请在后端配置 analytics.dify.base-url 和 analytics.dify.api-key，或设置环境变量 DIFY_BASE_URL、DIFY_API_KEY。
                """.formatted(normalizeMode(request.mode()), request.query(), compactContext(request.context()));
    }

    private String compactContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "未传入页面上下文。";
        }
        return context.entrySet().stream()
                .limit(30)
                .map(entry -> "- " + entry.getKey() + ": " + valueToText(entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String valueToText(Object value) {
        if (value == null) return "--";
        String text = String.valueOf(value);
        return text.length() > 1200 ? text.substring(0, 1200) + "...（已截断）" : text;
    }

    private String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "general" : mode;
    }

    private static String valueAsString(Map<?, ?> map, String key, String fallback) {
        if (map == null || map.get(key) == null) {
            return fallback;
        }
        return String.valueOf(map.get(key));
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
