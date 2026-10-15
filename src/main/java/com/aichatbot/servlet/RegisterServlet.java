package com.aichatbot.servlet;

import com.aichatbot.model.User;
import com.aichatbot.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/register")
public class RegisterServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (user != null) {
            redirect(req, resp, homeFor(user));
            return;
        }
        forward(req, resp, "register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fullName = param(req, "fullName");
        String email = param(req, "email");

        Map<String, String> errors = authService.register(fullName, email,
                req.getParameter("password"), req.getParameter("confirmPassword"));
        if (errors.isEmpty()) {
            flash(req, "success", "Registration successful. Please log in.");
            redirect(req, resp, "/login");            // Post/Redirect/Get
            return;
        }
        Map<String, String> form = new HashMap<>();
        form.put("fullName", fullName);
        form.put("email", email);
        req.setAttribute("form", form);
        req.setAttribute("errors", errors);
        forward(req, resp, "register.jsp");
    }
}
