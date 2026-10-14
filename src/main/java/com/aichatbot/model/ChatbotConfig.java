package com.aichatbot.model;

import java.sql.Timestamp;

public class ChatbotConfig extends BaseEntity {

    private String botName;
    private String welcomeMessage;
    private String fallbackResponse;
    private Timestamp updatedAt;

    public String getBotName() { return botName; }
    public void setBotName(String botName) { this.botName = botName; }
    public String getWelcomeMessage() { return welcomeMessage; }
    public void setWelcomeMessage(String welcomeMessage) { this.welcomeMessage = welcomeMessage; }
    public String getFallbackResponse() { return fallbackResponse; }
    public void setFallbackResponse(String fallbackResponse) { this.fallbackResponse = fallbackResponse; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
