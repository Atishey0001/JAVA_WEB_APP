package com.aichatbot.filter;

import com.aichatbot.util.CsrfUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Safe methods (GET/HEAD/OPTIONS) make sure the session has a CSRF token for the forms to embed.
 * Every other method (POST...) must carry the matching token or it is rejected with 403.
 */
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (SAFE_METHODS.contains(request.getMethod())) {
            CsrfUtil.ensureToken(request.getSession(true));
            chain.doFilter(req, res);
            return;
        }
        if (!CsrfUtil.isValid(request)) {
            request.setAttribute("errorMessage",
                    "Your session expired or the form could not be verified. Please go back, refresh the page and try again.");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(request, response);
            return;
        }
        chain.doFilter(req, res);
    }
}
