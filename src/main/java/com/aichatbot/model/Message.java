package com.aichatbot.model;

public class Message extends BaseEntity {

    public static final String SENDER_USER = "USER";
    public static final String SENDER_BOT = "BOT";

    private int conversationId;
    private String sender;
    private String content;
    private Integer matchedEntryId;
    private boolean unanswered;

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getMatchedEntryId() { return matchedEntryId; }
    public void setMatchedEntryId(Integer matchedEntryId) { this.matchedEntryId = matchedEntryId; }
    public boolean isUnanswered() { return unanswered; }
    public void setUnanswered(boolean unanswered) { this.unanswered = unanswered; }

    /** Used from JSP as ${message.fromUser}. */
    public boolean isFromUser() { return SENDER_USER.equals(sender); }
}
