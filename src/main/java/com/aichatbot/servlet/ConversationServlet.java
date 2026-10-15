package com.aichatbot.servlet;

import com.aichatbot.dao.ConversationDAO;
import com.aichatbot.dao.MessageDAO;
import com.aichatbot.exception.ChatbotException;
import com.aichatbot.model.Conversation;
import com.aichatbot.model.User;
import com.aichatbot.service.ChatbotService;
import com.aichatbot.service.ChatbotServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * One servlet, three read-only pages (chosen by URL):
 *  /user/history        - the logged-in user's conversations
 *  /user/conversation   - one conversation (ownership checked)
 *  /admin/conversations - admin search + inspection of every conversation
 */
@WebServlet({"/user/history", "/user/conversation", "/admin/conversations"})
public class ConversationServlet extends BaseServlet {

    private final ChatbotService chatbotService = new ChatbotServiceImpl();
    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        switch (req.getServletPath()) {
            case "/user/history" -> {
                req.setAttribute("conversations", chatbotService.listConversations(user.getId()));
                forward(req, resp, "user/history.jsp");
            }
            case "/user/conversation" -> showUserConversation(req, resp, user);
            default -> showAdminConversations(req, resp);
        }
    }

    private void showUserConversation(HttpServletRequest req, HttpServletResponse resp, User user)
            throws ServletException, IOException {
        Integer id = parseId(req.getParameter("id"));
        try {
            if (id == null) {
                throw new ChatbotException("Conversation not found.");
            }
            req.setAttribute("conversation", chatbotService.getConversation(id, user.getId()));
            req.setAttribute("messages", chatbotService.getMessages(id, user.getId()));
            forward(req, resp, "user/conversation.jsp");
        } catch (ChatbotException e) {
            flash(req, "error", e.getMessage());
            redirect(req, resp, "/user/history");
        }
    }

    private void showAdminConversations(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String query = param(req, "q");
        req.setAttribute("q", query);
        req.setAttribute("conversations", conversationDAO.search(query));

        Integer id = parseId(req.getParameter("id"));
        if (id != null) {
            Conversation selected = conversationDAO.findById(id);
            if (selected == null) {
                flash(req, "error", "Conversation not found.");
                redirect(req, resp, "/admin/conversations");
                return;
            }
            req.setAttribute("selected", selected);
            req.setAttribute("messages", messageDAO.findByConversation(id));
        }
        forward(req, resp, "admin/conversations.jsp");
    }
}
