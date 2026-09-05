package com.moveinsync.opspulse.sarvam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moveinsync.opspulse.ai.AiCostService;
import com.moveinsync.opspulse.config.SarvamProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SarvamSpeechClient {

    private static final Logger log = LoggerFactory.getLogger(SarvamSpeechClient.class);

    private final SarvamProperties properties;
    private final AiCostService aiCostService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public SarvamSpeechClient(
            SarvamProperties properties,
            AiCostService aiCostService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.aiCostService = aiCostService;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public TranscriptionResult transcribe(byte[] audioBytes, String filename, String contentType) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Sarvam API key not configured. Set SARVAM_API_KEY in docker-compose.yml or .env");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename != null ? filename : "audio.webm";
            }
        });
        body.add("model", properties.getSttModel());
        body.add("mode", "transcribe");
        body.add("language_code", properties.getLanguageCode());

        try {
            String response = restClient.post()
                    .uri(properties.getBaseUrl() + "/speech-to-text")
                    .header("api-subscription-key", properties.getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode transcript = root.get("transcript");
            if (transcript == null || transcript.isNull() || transcript.asText().isBlank()) {
                throw new IllegalStateException("Sarvam returned empty transcript");
            }

            String text = transcript.asText().trim();
            Integer durationMs = parseDurationMs(root);
            int inputTokens = aiCostService.estimateAudioInputTokens(audioBytes.length, durationMs);
            int outputTokens = aiCostService.estimateTextTokens(text);

            aiCostService.logSttUsage(properties.getSttModel(), audioBytes.length, text, durationMs);

            return new TranscriptionResult(text, inputTokens, outputTokens, durationMs);
        } catch (RestClientException e) {
            log.error("Sarvam STT request failed", e);
            throw new IllegalStateException("Speech-to-text failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse Sarvam response", e);
            throw new IllegalStateException("Speech-to-text failed: " + e.getMessage(), e);
        }
    }

    private Integer parseDurationMs(JsonNode root) {
        if (root.has("duration_ms") && !root.get("duration_ms").isNull()) {
            return root.get("duration_ms").asInt();
        }
        if (root.has("audio_duration") && !root.get("audio_duration").isNull()) {
            return (int) (root.get("audio_duration").asDouble() * 1000);
        }
        return null;
    }
}
