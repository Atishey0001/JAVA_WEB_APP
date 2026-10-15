package com.aichatbot.filter;

import com.aichatbot.model.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/** Runs after AuthenticationFilter: only users with role ADMIN may open /admin/*. */
public class AdminAuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        HttpSession session = request.getSession(false);
        Object attribute = session == null ? null : session.getAttribute(AuthenticationFilter.SESSION_USER);
        boolean admin = attribute instanceof User user && user.isAdmin();
        if (!admin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/WEB-INF/views/access-denied.jsp").forward(request, response);
            return;
        }
        chain.doFilter(req, res);
    }
}
