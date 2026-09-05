package com.moveinsync.opspulse.api.dto;

public class ChatResponse {

    private String reply;
    private String transcript;
    private boolean speechUsed;

    public ChatResponse() {
    }

    public ChatResponse(String reply) {
        this.reply = reply;
        this.speechUsed = false;
    }

    public ChatResponse(String reply, String transcript) {
        this.reply = reply;
        this.transcript = transcript;
        this.speechUsed = transcript != null && !transcript.isBlank();
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public boolean isSpeechUsed() {
        return speechUsed;
    }

    public void setSpeechUsed(boolean speechUsed) {
        this.speechUsed = speechUsed;
    }
}
