package com.moveinsync.opspulse.sarvam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public SarvamSpeechClient(SarvamProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public String transcribe(byte[] audioBytes, String filename, String contentType) {
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
            return transcript.asText().trim();
        } catch (RestClientException e) {
            log.error("Sarvam STT request failed", e);
            throw new IllegalStateException("Speech-to-text failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse Sarvam response", e);
            throw new IllegalStateException("Speech-to-text failed: " + e.getMessage(), e);
        }
    }
}
