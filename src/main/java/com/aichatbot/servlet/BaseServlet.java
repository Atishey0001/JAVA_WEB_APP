package com.aichatbot.servlet;

import com.aichatbot.filter.AuthenticationFilter;
import com.aichatbot.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Inheritance with a purpose: helpers shared by all servlets. */
public abstract class BaseServlet extends HttpServlet {

    protected User currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object user = session == null ? null : session.getAttribute(AuthenticationFilter.SESSION_USER);
        return user instanceof User u ? u : null;
    }

    /** Servlets prepare data as request attributes, then forward to a JSP hidden under WEB-INF. */
    protected void forward(HttpServletRequest req, HttpServletResponse resp, String view)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view).forward(req, resp);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected String homeFor(User user) {
        return user.isAdmin() ? "/admin/dashboard" : "/user/dashboard";
    }

    /** One-time message shown after a redirect (Post/Redirect/Get). type: success | error | info */
    protected void flash(HttpServletRequest req, String type, String message) {
        HttpSession session = req.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashMessage", message);
    }

    protected Integer parseId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
