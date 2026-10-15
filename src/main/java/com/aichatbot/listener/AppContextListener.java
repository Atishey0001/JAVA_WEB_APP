package com.aichatbot.listener;

import com.aichatbot.service.AnalyticsService;
import com.aichatbot.service.AuthService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs when Tomcat starts / stops the application.
 * Start: creates the shared AnalyticsService and, if the environment variables ADMIN_EMAIL and
 * ADMIN_PASSWORD are set and no admin exists yet, creates the first administrator.
 * Stop: shuts the analytics thread pool down.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    public static final String ANALYTICS_ATTRIBUTE = "analyticsService";
    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.setAttribute(ANALYTICS_ATTRIBUTE, new AnalyticsService());

        String email = System.getenv("ADMIN_EMAIL");
        String password = System.getenv("ADMIN_PASSWORD");
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            LOG.info("ADMIN_EMAIL / ADMIN_PASSWORD not set - no admin account will be created at startup.");
            return;
        }
        try {
            boolean created = new AuthService().createAdminIfMissing(System.getenv("ADMIN_NAME"), email, password);
            LOG.info(created ? "Initial admin account created for " + email : "An admin already exists - nothing to do.");
        } catch (RuntimeException e) {
            // Do not stop the application (e.g. MySQL not running yet); just report it in the log.
            LOG.log(Level.SEVERE, "Could not create the initial admin account: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        Object analytics = event.getServletContext().getAttribute(ANALYTICS_ATTRIBUTE);
        if (analytics instanceof AnalyticsService service) {
            service.shutdown();
        }
    }
}
