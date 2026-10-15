package com.aichatbot.servlet;

import com.aichatbot.model.User;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import com.aichatbot.service.FeedbackService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** /user/feedback (submit + own history) and /admin/feedback (review, filter by rating). */
@WebServlet({"/user/feedback", "/admin/feedback"})
public class FeedbackServlet extends BaseServlet {

    private final FeedbackService feedbackService = new FeedbackService();
    private final ChatbotService chatbotService = new ChatbotServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/admin/feedback".equals(req.getServletPath())) {
            Integer rating = parseId(req.getParameter("rating"));
            if (rating != null && (rating < 1 || rating > 5)) {
                rating = null;
            }
            req.setAttribute("ratingFilter", rating);
            req.setAttribute("feedbackList", feedbackService.listAll(rating));
            req.setAttribute("averageRating", feedbackService.averageRating());
            req.setAttribute("distribution", feedbackService.ratingDistribution());
            forward(req, resp, "admin/feedback.jsp");
            return;
        }
        showUserForm(req, resp, currentUser(req));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/admin/feedback".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        User user = currentUser(req);
        String rating = param(req, "rating");
        String comment = param(req, "comment");
        String conversationId = param(req, "conversationId");

        Map<String, String> errors = feedbackService.submit(user.getId(), rating, comment, conversationId);
        if (errors.isEmpty()) {
            flash(req, "success", "Thank you! Your feedback was submitted.");
            redirect(req, resp, "/user/feedback");               // Post/Redirect/Get
            return;
        }
        Map<String, String> form = new HashMap<>();
        form.put("rating", rating);
        form.put("comment", comment);
        form.put("conversationId", conversationId);
        req.setAttribute("form", form);
        req.setAttribute("errors", errors);
        showUserForm(req, resp, user);
    }

    private void showUserForm(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        req.setAttribute("conversations", chatbotService.listConversations(user.getId()));
        req.setAttribute("myFeedback", feedbackService.listForUser(user.getId()));
        forward(req, resp, "user/feedback.jsp");
    }
}
