package com.aichatbot.exception;

/** Checked exception whose message is safe to show to the end user (validation, ownership...). */
public class ChatbotException extends Exception {
    private static final long serialVersionUID = 1L;

    public ChatbotException(String message) {
        super(message);
    }
}
