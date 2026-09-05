package com.moveinsync.opspulse.api;

import com.moveinsync.opspulse.api.dto.ChatResponse;
import com.moveinsync.opspulse.chat.ChatService;
import com.moveinsync.opspulse.config.SarvamProperties;
import com.moveinsync.opspulse.sarvam.SarvamSpeechClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final SarvamSpeechClient sarvamSpeechClient;
    private final SarvamProperties sarvamProperties;

    public ChatController(
            ChatService chatService,
            SarvamSpeechClient sarvamSpeechClient,
            SarvamProperties sarvamProperties) {
        this.chatService = chatService;
        this.sarvamSpeechClient = sarvamSpeechClient;
        this.sarvamProperties = sarvamProperties;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "sarvamConfigured", sarvamProperties.isConfigured(),
                "hint", sarvamProperties.isConfigured()
                        ? "Speech input ready"
                        : "Set SARVAM_API_KEY in opspulse/.env or docker-compose.yml");
    }

    @PostMapping
    public ChatResponse chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        return new ChatResponse(chatService.reply(message));
    }

    @PostMapping(value = "/speech", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatFromSpeech(@RequestPart("audio") MultipartFile audio) throws Exception {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is empty");
        }

        String transcript = sarvamSpeechClient.transcribe(
                audio.getBytes(),
                audio.getOriginalFilename(),
                audio.getContentType());

        String reply = chatService.reply(transcript);
        return new ChatResponse(reply, transcript);
    }
}
