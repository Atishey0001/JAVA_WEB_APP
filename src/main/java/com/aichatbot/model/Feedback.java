package com.aichatbot.model;

public class Feedback extends BaseEntity {

    private int userId;
    private Integer conversationId;
    private int rating;
    private String comment;
    private String userName; // filled by joined queries

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public Integer getConversationId() { return conversationId; }
    public void setConversationId(Integer conversationId) { this.conversationId = conversationId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
