package com.aichatbot.servlet;

import com.aichatbot.model.ChatbotConfig;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/configuration")
public class ConfigurationServlet extends BaseServlet {

    private final ChatbotService chatbotService = new ChatbotServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("config", chatbotService.getConfig());
        forward(req, resp, "admin/configuration.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ChatbotConfig config = new ChatbotConfig();
        config.setBotName(param(req, "botName"));
        config.setWelcomeMessage(param(req, "welcomeMessage"));
        config.setFallbackResponse(param(req, "fallbackResponse"));

        Map<String, String> errors = chatbotService.updateConfig(config);
        if (errors.isEmpty()) {
            flash(req, "success", "Chatbot configuration saved.");
            redirect(req, resp, "/admin/configuration");
            return;
        }
        req.setAttribute("config", config);
        req.setAttribute("errors", errors);
        forward(req, resp, "admin/configuration.jsp");
    }
}
