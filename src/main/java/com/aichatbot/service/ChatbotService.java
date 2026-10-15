package com.aichatbot.service;

import com.aichatbot.exception.ChatbotException;
import com.aichatbot.model.ChatbotConfig;
import com.aichatbot.model.Conversation;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.model.Message;

import java.util.List;
import java.util.Map;

/** Abstraction: servlets depend on this interface, not on the implementation. */
public interface ChatbotService {

    /** The chosen answer text and the knowledge-base entry id it came from (null = fallback). */
    record Answer(String text, Integer entryId) {
    }

    /**
     * Validates the question, finds an answer, and stores both messages in one transaction.
     *
     * @param conversationId existing conversation id, or null to start a new conversation
     * @return the id of the conversation the messages were stored in
     */
    int sendMessage(int userId, Integer conversationId, String question) throws ChatbotException;

    /** Pure matching logic (no database access) - easy to unit test. */
    Answer findAnswer(String question, List<KnowledgeBaseEntry> entries, String fallbackResponse);

    /** @throws ChatbotException if the conversation does not exist or belongs to another user */
    Conversation getConversation(int conversationId, int userId) throws ChatbotException;

    List<Message> getMessages(int conversationId, int userId) throws ChatbotException;

    List<Conversation> listConversations(int userId);

    ChatbotConfig getConfig();

    /** @return field -> error message; empty map means the configuration was saved */
    Map<String, String> updateConfig(ChatbotConfig config);
}
