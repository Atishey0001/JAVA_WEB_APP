package com.aichatbot.service;

import com.aichatbot.dao.ChatbotConfigDAO;
import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.KnowledgeBaseDAO;
import com.aichatbot.dao.MessageDAO;
import com.aichatbot.exception.ChatbotException;
import com.aichatbot.model.ChatbotConfig;
import com.aichatbot.model.Conversation;
import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.model.Message;
import com.aichatbot.util.TextNormalizer;
import com.aichatbot.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ChatbotServiceImpl implements ChatbotService {

    static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 50;

    private final ConversationDAO conversationDAO;
    private final MessageDAO messageDAO;
    private final KnowledgeBaseDAO knowledgeBaseDAO;
    private final ChatbotConfigDAO configDAO;
    /** Tried in order; the first strategy that finds a match wins. */
    private final List<ResponseStrategy> strategies = List.of(new KeywordMatchStrategy(), new TokenOverlapStrategy());

    public ChatbotServiceImpl() {
        this(new ConversationDAO(), new MessageDAO(), new KnowledgeBaseDAO(), new ChatbotConfigDAO());
    }

    public ChatbotServiceImpl(ConversationDAO conversationDAO, MessageDAO messageDAO,
                              KnowledgeBaseDAO knowledgeBaseDAO, ChatbotConfigDAO configDAO) {
        this.conversationDAO = conversationDAO;
        this.messageDAO = messageDAO;
        this.knowledgeBaseDAO = knowledgeBaseDAO;
        this.configDAO = configDAO;
    }

    @Override
    public int sendMessage(int userId, Integer conversationId, String question) throws ChatbotException {
        String text = question == null ? "" : question.trim();
        if (text.isEmpty()) {
            throw new ChatbotException("Please type a question before sending.");
        }
        if (text.length() > MAX_QUESTION_LENGTH) {
            throw new ChatbotException("Your message is too long (maximum " + MAX_QUESTION_LENGTH + " characters).");
        }

        int targetConversation;
        if (conversationId == null) {
            targetConversation = conversationDAO.create(userId, makeTitle(text));
        } else {
            if (conversationDAO.findByIdAndUser(conversationId, userId) == null) {
                throw new ChatbotException("Conversation not found.");
            }
            targetConversation = conversationId;
        }

        ChatbotConfig config = configDAO.get();
        Answer answer = findAnswer(text, knowledgeBaseDAO.findActive(), config.getFallbackResponse());
        messageDAO.saveExchange(targetConversation, text, answer.text(), answer.entryId());
        return targetConversation;
    }

    @Override
    public Answer findAnswer(String question, List<KnowledgeBaseEntry> entries, String fallbackResponse) {
        String normalized = TextNormalizer.normalize(question);
        for (ResponseStrategy strategy : strategies) {
            Optional<KnowledgeBaseEntry> match = strategy.findMatch(normalized, entries);
            if (match.isPresent()) {
                return new Answer(match.get().getAnswer(), match.get().getId());
            }
        }
        return new Answer(fallbackResponse, null);
    }

    @Override
    public Conversation getConversation(int conversationId, int userId) throws ChatbotException {
        Conversation conversation = conversationDAO.findByIdAndUser(conversationId, userId);
        if (conversation == null) {
            throw new ChatbotException("Conversation not found.");
        }
        return conversation;
    }

    @Override
    public List<Message> getMessages(int conversationId, int userId) throws ChatbotException {
        getConversation(conversationId, userId); // ownership check
        return messageDAO.findByConversation(conversationId);
    }

    @Override
    public List<Conversation> listConversations(int userId) {
        return conversationDAO.findByUser(userId);
    }

    @Override
    public ChatbotConfig getConfig() {
        return configDAO.get();
    }

    @Override
    public Map<String, String> updateConfig(ChatbotConfig config) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (Validator.isBlank(config.getBotName())) {
            errors.put("botName", "Chatbot name is required.");
        } else if (config.getBotName().trim().length() > 50) {
            errors.put("botName", "Chatbot name must be at most 50 characters.");
        }
        if (Validator.isBlank(config.getWelcomeMessage())) {
            errors.put("welcomeMessage", "Welcome message is required.");
        } else if (config.getWelcomeMessage().trim().length() > 500) {
            errors.put("welcomeMessage", "Welcome message must be at most 500 characters.");
        }
        if (Validator.isBlank(config.getFallbackResponse())) {
            errors.put("fallbackResponse", "Fallback response is required.");
        } else if (config.getFallbackResponse().trim().length() > 500) {
            errors.put("fallbackResponse", "Fallback response must be at most 500 characters.");
        }
        if (errors.isEmpty()) {
            config.setBotName(config.getBotName().trim());
            config.setWelcomeMessage(config.getWelcomeMessage().trim());
            config.setFallbackResponse(config.getFallbackResponse().trim());
            configDAO.save(config);
        }
        return errors;
    }

    private String makeTitle(String text) {
        return text.length() <= MAX_TITLE_LENGTH ? text : text.substring(0, MAX_TITLE_LENGTH - 3) + "...";
    }
}
