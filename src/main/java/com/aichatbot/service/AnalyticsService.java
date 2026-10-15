package com.aichatbot.service;

import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.FeedbackDAO;
import com.aichatbot.dao.MessageDAO;
import com.aichatbot.dao.UserDAO;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Builds the admin dashboard numbers from the database. Multithreading: the three groups of
 * queries are independent, so an ExecutorService runs them at the same time on separate
 * connections and the results are merged into one Map.
 */
public class AnalyticsService {

    private static final int RECENT_LIMIT = 5;
    private static final int LOW_RATING_MAX = 2;

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final UserDAO userDAO = new UserDAO();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final FeedbackDAO feedbackDAO = new FeedbackDAO();

    public Map<String, Object> loadDashboard() {
        List<Callable<Map<String, Object>>> tasks = List.of(this::loadCounts, this::loadFeedbackStats, this::loadImprovementData);
        Map<String, Object> stats = new HashMap<>();
        try {
            for (Future<Map<String, Object>> result : executor.invokeAll(tasks)) {
                stats.putAll(result.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Analytics was interrupted", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Analytics failed", e.getCause());
        }
        return stats;
    }

    private Map<String, Object> loadCounts() {
        Map<String, Object> part = new HashMap<>();
        part.put("totalUsers", userDAO.count());
        part.put("totalConversations", conversationDAO.count());
        part.put("totalMessages", messageDAO.count());
        part.put("recentConversations", conversationDAO.findRecent(RECENT_LIMIT));
        return part;
    }

    private Map<String, Object> loadFeedbackStats() {
        Map<String, Object> part = new HashMap<>();
        part.put("totalFeedback", feedbackDAO.count());
        part.put("averageRating", feedbackDAO.averageRating());
        part.put("recentFeedback", feedbackDAO.findRecent(RECENT_LIMIT));
        return part;
    }

    private Map<String, Object> loadImprovementData() {
        Map<String, Object> part = new HashMap<>();
        part.put("unansweredQuestions", messageDAO.findTopUnanswered(10));
        part.put("lowRatedFeedback", feedbackDAO.findLowRated(LOW_RATING_MAX, 10));
        return part;
    }

    /** Called when the web application stops. */
    public void shutdown() {
        executor.shutdown();
    }
}
