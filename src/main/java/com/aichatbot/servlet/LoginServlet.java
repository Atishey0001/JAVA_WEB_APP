package com.aichatbot.servlet;

import com.aichatbot.filter.AuthenticationFilter;
import com.aichatbot.model.User;
import com.aichatbot.service.AuthService;
import com.aichatbot.util.CsrfUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (user != null) {
            redirect(req, resp, homeFor(user));
            return;
        }
        forward(req, resp, "login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = param(req, "email");
        String password = req.getParameter("password");

        User user = authService.login(email, password);
        if (user == null) {
            req.setAttribute("error", "Invalid email or password.");
            req.setAttribute("email", email);
            forward(req, resp, "login.jsp");
            return;
        }
        HttpSession session = req.getSession(true);
        req.changeSessionId();                 // new session id after login (prevents session fixation)
        session.setAttribute(AuthenticationFilter.SESSION_USER, user);
        CsrfUtil.regenerate(session);
        redirect(req, resp, homeFor(user));
    }
}
