package com.aichatbot.model;

import java.sql.Timestamp;

public class KnowledgeBaseEntry extends BaseEntity {

    private String title;
    private String keywords;
    private String answer;
    private String category = "General";
    private boolean active = true;
    private Timestamp updatedAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
