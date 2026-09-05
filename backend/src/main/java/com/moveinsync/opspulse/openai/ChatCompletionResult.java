package com.moveinsync.opspulse.openai;

public class ChatCompletionResult {

    private final String content;
    private final int inputTokens;
    private final int outputTokens;
    private final String model;

    public ChatCompletionResult(String content, int inputTokens, int outputTokens, String model) {
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.model = model;
    }

    public String getContent() {
        return content;
    }

    public int getInputTokens() {
        return inputTokens;
    }

    public int getOutputTokens() {
        return outputTokens;
    }

    public String getModel() {
        return model;
    }
}
