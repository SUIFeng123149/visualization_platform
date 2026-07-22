package com.bililens.analytics.ai.service;

import com.bililens.analytics.ai.dto.AiChatRequest;
import com.bililens.analytics.ai.dto.AiChatResponse;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String defaultUser;
    private final ExecutorService executor = Executors.newCachedThreadPool(task -> {
        Thread thread = new Thread(task, "ai-assistant-stream");
        thread.setDaemon(true);
        return thread;
    });

    public AiAssistantService(
            @Value("${analytics.dify.base-url:}") String baseUrl,
            @Value("${analytics.dify.api-key:}") String apiKey,
            @Value("${analytics.dify.user:bililens-user}") String defaultUser,
            ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.defaultUser = defaultUser;
    }

    @PreDestroy
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            return new AiChatResponse("请输入要咨询的问题。", request.conversationId(), hasDifyConfig());
        }

        if (!hasDifyConfig()) {
            return new AiChatResponse(buildLocalAnswer(request), null, false);
        }

        Map<String, Object> response = restClient.post()
                .uri(baseUrl + "/chat-messages")
                .header("Authorization", "Bearer " + apiKey)
                .body(buildDifyBody(request, "blocking"))
                .retrieve()
                .body(Map.class);

        String answer = valueAsString(response, "answer", "Dify 未返回 answer 字段。");
        String conversationId = valueAsString(response, "conversation_id", request.conversationId());
        return new AiChatResponse(answer, conversationId, true);
    }

    public SseEmitter chatStream(AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        AtomicBoolean completed = new AtomicBoolean(false);

        if (request.query() == null || request.query().isBlank()) {
            sendAndComplete(emitter, "error", "请输入要咨询的问题。");
            return emitter;
        }

        if (!hasDifyConfig()) {
            sendAndComplete(emitter, "message", buildLocalAnswer(request));
            return emitter;
        }

        CompletableFuture.runAsync(() -> streamFromDify(request, emitter, completed), executor);

        emitter.onCompletion(() -> completed.set(true));
        emitter.onTimeout(() -> {
            completed.set(true);
            emitter.complete();
        });
        emitter.onError(ex -> completed.set(true));

        return emitter;
    }

    private void streamFromDify(AiChatRequest request, SseEmitter emitter, AtomicBoolean completed) {
        try {
            HttpURLConnection connection = openDifyStreamConnection(request);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                String errorBody = connection.getErrorStream() == null
                        ? ""
                        : new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                sendAndComplete(emitter, "error", "Dify API 请求失败，状态码 " + status + "：" + errorBody);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && !completed.get()) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if (payload.isBlank() || "[DONE]".equals(payload)) {
                        continue;
                    }
                    handleDifySsePayload(payload, emitter, completed);
                }
            }

            if (!completed.get()) {
                emitter.send(SseEmitter.event().name("done").data("{}"));
                emitter.complete();
                completed.set(true);
            }
        } catch (Exception ex) {
            if (!completed.get()) {
                sendAndComplete(emitter, "error", ex.getMessage() == null ? "AI 流式请求失败" : ex.getMessage());
            }
        }
    }

    private HttpURLConnection openDifyStreamConnection(AiChatRequest request) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(baseUrl + "/chat-messages").toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "text/event-stream");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(5 * 60 * 1000);

        String body = objectMapper.writeValueAsString(buildDifyBody(request, "streaming"));
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return connection;
    }

    private void handleDifySsePayload(String payload, SseEmitter emitter, AtomicBoolean completed) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String event = node.path("event").asText("");

        switch (event) {
            case "message", "agent_message" -> {
                String answer = node.path("answer").asText("");
                if (!answer.isEmpty()) {
                    emitter.send(SseEmitter.event().name("message").data(normalizeChineseColon(answer)));
                }
            }
            case "message_end" -> {
                String conversationId = node.path("conversation_id").asText("");
                emitter.send(SseEmitter.event().name("done").data(Map.of("conversation_id", conversationId)));
                emitter.complete();
                completed.set(true);
            }
            case "message_file" -> emitter.send(SseEmitter.event().name("file").data(payload));
            case "error" -> {
                String message = node.path("message").asText("Dify 返回错误");
                sendAndComplete(emitter, "error", message);
                completed.set(true);
            }
            default -> {
                // Ignore ping, workflow events, and metadata-only chunks.
            }
        }
    }

    private Map<String, Object> buildDifyBody(AiChatRequest request, String responseMode) {
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", Map.of());
        body.put("query", buildPrompt(request));
        body.put("response_mode", responseMode);
        body.put("user", request.user() == null || request.user().isBlank() ? defaultUser : request.user());
        body.put("files", java.util.List.of());
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            body.put("conversation_id", request.conversationId());
        }
        return body;
    }

    private void sendAndComplete(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(normalizeEventData(eventName, data)));
            if (!"done".equals(eventName)) {
                emitter.send(SseEmitter.event().name("done").data("{}"));
            }
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private boolean hasDifyConfig() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    private Object normalizeEventData(String eventName, Object data) {
        if (("message".equals(eventName) || "error".equals(eventName)) && data instanceof String text) {
            return normalizeChineseColon(text);
        }
        return data;
    }

    private String normalizeChineseColon(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text
                .replaceAll("(?m)^([\\p{IsHan}A-Za-z0-9_（）()《》【】“”'\\\"-]{1,40})\\s*$\\R\\s*[：:]\\s*", "$1：")
                .replaceAll("(?m)^\\s*[：:]\\s*", "")
                .replaceAll("([\\p{IsHan}])\\s+[：:]", "$1：");
    }

    private String buildPrompt(AiChatRequest request) {
        return """
                你是 BiliLens 数据可视化平台的 AI 数据分析助手。
                请基于用户问题和页面上下文回答，优先给出数据解释、原因判断和可执行建议。
                如果上下文不足，请明确说明缺少哪些数据，不要编造指标。
                请使用 Markdown 格式输出，可使用标题、列表、表格和代码块。
                Markdown 标题必须写成“## 结论”而不是“##结论”，无序列表必须写成“- 热度表现：...”而不是“-热度表现”。
                正文里的中文标签和字段请使用全角冒号，并让冒号紧跟标签文字，例如“重复率：暂无数据”。
                不要把冒号单独放在行首、行尾或下一行，也不要输出“结论\n：xxx”这类格式。
                列表编号必须写成“1. 内容”，不要输出“1.\n内容”。

                功能场景：%s
                用户问题：%s

                页面上下文：
                %s
                """.formatted(normalizeMode(request.mode()), request.query(), compactContext(request.context()));
    }

    private String buildLocalAnswer(AiChatRequest request) {
        return """
                ## Dify 尚未配置

                **场景**：%s

                **问题**：%s

                ### 当前页面上下文摘要
                %s

                如需启用真正的 AI 分析，请配置 `analytics.dify.base-url` 和 `analytics.dify.api-key`，或设置环境变量 `DIFY_BASE_URL`、`DIFY_API_KEY`。
                """.formatted(normalizeMode(request.mode()), request.query(), compactContext(request.context()));
    }

    private String compactContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "未传入页面上下文。";
        }
        return context.entrySet().stream()
                .limit(30)
                .map(entry -> "- " + entry.getKey() + "：" + valueToText(entry.getValue()))
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
