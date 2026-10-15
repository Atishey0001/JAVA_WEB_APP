package com.aichatbot.servlet;

import com.aichatbot.model.User;
import com.aichatbot.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

@WebServlet("/user/profile")
public class ProfileServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forward(req, resp, "user/profile.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        String action = param(req, "action");

        if ("updateProfile".equals(action)) {
            String fullName = param(req, "fullName");
            Map<String, String> errors = authService.updateProfile(user.getId(), fullName);
            if (errors.isEmpty()) {
                user.setFullName(fullName);             // keep the session copy in sync
                flash(req, "success", "Profile updated.");
                redirect(req, resp, "/user/profile");
                return;
            }
            req.setAttribute("profileErrors", errors);
        } else if ("changePassword".equals(action)) {
            Map<String, String> errors = authService.changePassword(user.getId(),
                    req.getParameter("currentPassword"), req.getParameter("newPassword"),
                    req.getParameter("confirmPassword"));
            if (errors.isEmpty()) {
                flash(req, "success", "Password changed.");
                redirect(req, resp, "/user/profile");
                return;
            }
            req.setAttribute("passwordErrors", errors);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        forward(req, resp, "user/profile.jsp");
    }
}
