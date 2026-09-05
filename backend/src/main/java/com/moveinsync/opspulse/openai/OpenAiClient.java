package com.moveinsync.opspulse.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.ai.AiCostService;
import com.moveinsync.opspulse.config.OpenAiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    public static final String PROVIDER = "OPENAI";

    private final OpenAiProperties properties;
    private final AiCostService aiCostService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiClient(OpenAiProperties properties, AiCostService aiCostService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.aiCostService = aiCostService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public ChatCompletionResult chat(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, true);
    }

    public ChatCompletionResult chatText(String systemPrompt, String userPrompt) {
        return complete(systemPrompt, userPrompt, false);
    }

    private ChatCompletionResult complete(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("OpenAI API key not configured. Set OPENAI_API_KEY in .env");
        }

        Map<String, Object> body = jsonMode
                ? Map.of(
                "model", properties.getModel(),
                "temperature", 0.3,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)))
                : Map.of(
                "model", properties.getModel(),
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        try {
            String response = restClient.post()
                    .uri(properties.getBaseUrl() + "/chat/completions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode usage = root.path("usage");
            int inputTokens = usage.path("prompt_tokens").asInt(0);
            int outputTokens = usage.path("completion_tokens").asInt(0);
            String model = root.path("model").asText(properties.getModel());
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            aiCostService.logOpenAiUsage(model, inputTokens, outputTokens);

            return new ChatCompletionResult(content, inputTokens, outputTokens, model);
        } catch (RestClientException e) {
            log.error("OpenAI request failed", e);
            throw new IllegalStateException("OpenAI request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse OpenAI response", e);
            throw new IllegalStateException("OpenAI request failed: " + e.getMessage(), e);
        }
    }
}
