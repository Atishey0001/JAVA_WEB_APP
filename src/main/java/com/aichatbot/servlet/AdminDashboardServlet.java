package com.aichatbot.servlet;

import com.aichatbot.listener.AppContextListener;
import com.aichatbot.model.User;
import com.aichatbot.service.AnalyticsService;
import com.aichatbot.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/** /admin/dashboard (statistics + improvement section) and /admin/users (list / delete users). */
@WebServlet({"/admin/dashboard", "/admin/users"})
public class AdminDashboardServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/admin/users".equals(req.getServletPath())) {
            req.setAttribute("users", authService.listUsers());
            forward(req, resp, "admin/users.jsp");
            return;
        }
        AnalyticsService analytics = (AnalyticsService) getServletContext()
                .getAttribute(AppContextListener.ANALYTICS_ATTRIBUTE);
        Map<String, Object> stats = analytics.loadDashboard();
        req.setAttribute("stats", stats);
        forward(req, resp, "admin/dashboard.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/admin/users".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        User admin = currentUser(req);
        Integer id = parseId(req.getParameter("id"));
        if (id == null || id == admin.getId()) {
            flash(req, "error", "You cannot delete that account.");
        } else if (authService.deleteUser(id)) {
            flash(req, "success", "User deleted together with their conversations and feedback.");
        } else {
            flash(req, "error", "User not found, or the account is an administrator.");
        }
        redirect(req, resp, "/admin/users");
    }
}
