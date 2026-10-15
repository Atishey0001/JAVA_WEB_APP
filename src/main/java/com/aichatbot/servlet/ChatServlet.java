package com.aichatbot.servlet;

import com.aichatbot.exception.ChatbotException;
import com.aichatbot.model.User;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** /user/chat           -> new, empty conversation
 *  /user/chat?id=7      -> continue conversation 7 (must belong to the logged-in user) */
@WebServlet("/user/chat")
public class ChatServlet extends BaseServlet {

    private final ChatbotService chatbotService = new ChatbotServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        Integer conversationId = parseId(req.getParameter("id"));
        try {
            prepareChatPage(req, user, conversationId);
        } catch (ChatbotException e) {
            flash(req, "error", e.getMessage());
            redirect(req, resp, "/user/history");
            return;
        }
        forward(req, resp, "user/chatbot.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        Integer conversationId = parseId(req.getParameter("conversationId"));
        String message = req.getParameter("message");
        try {
            int savedIn = chatbotService.sendMessage(user.getId(), conversationId, message);
            redirect(req, resp, "/user/chat?id=" + savedIn);        // Post/Redirect/Get
        } catch (ChatbotException e) {
            try {
                prepareChatPage(req, user, conversationId);
            } catch (ChatbotException notFound) {
                flash(req, "error", notFound.getMessage());
                redirect(req, resp, "/user/history");
                return;
            }
            req.setAttribute("error", e.getMessage());
            req.setAttribute("draft", message);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            forward(req, resp, "user/chatbot.jsp");
        }
    }

    private void prepareChatPage(HttpServletRequest req, User user, Integer conversationId) throws ChatbotException {
        req.setAttribute("config", chatbotService.getConfig());
        if (conversationId != null) {
            req.setAttribute("conversation", chatbotService.getConversation(conversationId, user.getId()));
            req.setAttribute("messages", chatbotService.getMessages(conversationId, user.getId()));
        }
    }
}
