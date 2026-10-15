package com.aichatbot.servlet;

import com.aichatbot.model.Conversation;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/user/dashboard")
public class DashboardServlet extends BaseServlet {

    private final ChatbotService chatbotService = new ChatbotServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Conversation> all = chatbotService.listConversations(currentUser(req).getId());
        req.setAttribute("config", chatbotService.getConfig());
        req.setAttribute("conversationCount", all.size());
        req.setAttribute("recentConversations", all.subList(0, Math.min(5, all.size())));
        forward(req, resp, "user/dashboard.jsp");
    }
}
