package com.aichatbot.service;

import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.FeedbackDAO;
import com.aichatbot.model.Feedback;
import com.aichatbot.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FeedbackService {

    private final FeedbackDAO feedbackDAO;
    private final ConversationDAO conversationDAO;

    public FeedbackService() {
        this(new FeedbackDAO(), new ConversationDAO());
    }

    public FeedbackService(FeedbackDAO feedbackDAO, ConversationDAO conversationDAO) {
        this.feedbackDAO = feedbackDAO;
        this.conversationDAO = conversationDAO;
    }

    /**
     * @param ratingText         "1".."5" as submitted by the form
     * @param conversationIdText optional; when present it must be one of the user's own conversations
     * @return field -> error message; an empty map means the feedback was stored
     */
    public Map<String, String> submit(int userId, String ratingText, String comment, String conversationIdText) {
        Map<String, String> errors = new LinkedHashMap<>();

        int rating = 0;
        try {
            rating = Integer.parseInt(ratingText == null ? "" : ratingText.trim());
        } catch (NumberFormatException e) {
            // handled by the range check below
        }
        if (rating < 1 || rating > 5) {
            errors.put("rating", "Please choose a rating from 1 to 5.");
        }
        String cleanComment = comment == null ? "" : comment.trim();
        if (cleanComment.length() > 1000) {
            errors.put("comment", "Comment must be at most 1000 characters.");
        }

        Integer conversationId = null;
        if (!Validator.isBlank(conversationIdText)) {
            try {
                conversationId = Integer.valueOf(conversationIdText.trim());
                if (conversationDAO.findByIdAndUser(conversationId, userId) == null) {
                    errors.put("conversationId", "Selected conversation was not found.");
                }
            } catch (NumberFormatException e) {
                errors.put("conversationId", "Selected conversation was not found.");
            }
        }

        if (errors.isEmpty()) {
            Feedback feedback = new Feedback();
            feedback.setUserId(userId);
            feedback.setConversationId(conversationId);
            feedback.setRating(rating);
            feedback.setComment(cleanComment.isEmpty() ? null : cleanComment);
            feedbackDAO.create(feedback);
        }
        return errors;
    }

    public List<Feedback> listForUser(int userId) {
        return feedbackDAO.findByUser(userId);
    }

    public List<Feedback> listAll(Integer ratingFilter) {
        return feedbackDAO.findAll(ratingFilter);
    }

    public double averageRating() {
        return feedbackDAO.averageRating();
    }

    public Map<Integer, Integer> ratingDistribution() {
        return feedbackDAO.ratingDistribution();
    }
}
