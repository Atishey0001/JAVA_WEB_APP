package com.aichatbot.servlet;

import com.aichatbot.model.KnowledgeBaseEntry;
import com.aichatbot.service.KnowledgeBaseService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** Admin CRUD for the chatbot's knowledge base. */
@WebServlet("/admin/knowledge")
public class KnowledgeBaseServlet extends BaseServlet {

    private final KnowledgeBaseService service = new KnowledgeBaseService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        if ("new".equals(action)) {
            KnowledgeBaseEntry blank = new KnowledgeBaseEntry();
            blank.setKeywords(param(req, "keywords"));
            req.setAttribute("entry", blank);
            forward(req, resp, "admin/knowledge-form.jsp");
        } else if ("edit".equals(action)) {
            Integer id = parseId(req.getParameter("id"));
            KnowledgeBaseEntry entry = id == null ? null : service.find(id);
            if (entry == null) {
                flash(req, "error", "Knowledge-base entry not found.");
                redirect(req, resp, "/admin/knowledge");
                return;
            }
            req.setAttribute("entry", entry);
            forward(req, resp, "admin/knowledge-form.jsp");
        } else {
            String q = param(req, "q");
            req.setAttribute("q", q);
            req.setAttribute("entries", service.list(q));
            forward(req, resp, "admin/knowledge-base.jsp");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = param(req, "action");
        if ("delete".equals(action)) {
            Integer id = parseId(req.getParameter("id"));
            boolean deleted = id != null && service.delete(id);
            flash(req, deleted ? "success" : "error", deleted ? "Entry deleted." : "Entry not found.");
            redirect(req, resp, "/admin/knowledge");
        } else if ("save".equals(action)) {
            KnowledgeBaseEntry entry = new KnowledgeBaseEntry();
            Integer id = parseId(req.getParameter("id"));
            entry.setId(id == null ? 0 : id);
            entry.setTitle(param(req, "title"));
            entry.setKeywords(param(req, "keywords"));
            entry.setAnswer(param(req, "answer"));
            entry.setCategory(param(req, "category"));
            entry.setActive(req.getParameter("active") != null);

            Map<String, String> errors = service.save(entry);
            if (errors.isEmpty()) {
                flash(req, "success", id == null ? "Entry added." : "Entry updated.");
                redirect(req, resp, "/admin/knowledge");
                return;
            }
            req.setAttribute("entry", entry);
            req.setAttribute("errors", errors);
            forward(req, resp, "admin/knowledge-form.jsp");
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
