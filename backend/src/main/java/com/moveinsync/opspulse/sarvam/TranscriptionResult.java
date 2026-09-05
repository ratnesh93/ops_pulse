package com.moveinsync.opspulse.sarvam;

public class TranscriptionResult {

    private final String transcript;
    private final int inputTokens;
    private final int outputTokens;
    private final Integer audioDurationMs;

    public TranscriptionResult(String transcript, int inputTokens, int outputTokens, Integer audioDurationMs) {
        this.transcript = transcript;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.audioDurationMs = audioDurationMs;
    }

    public String getTranscript() {
        return transcript;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public Integer getAudioDurationMs() {
        return audioDurationMs;
    }
}
