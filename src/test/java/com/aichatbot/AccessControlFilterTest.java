package com.aichatbot;

import com.aichatbot.filter.AdminAuthorizationFilter;
import com.aichatbot.filter.AuthenticationFilter;
import com.aichatbot.filter.CsrfFilter;
import com.aichatbot.model.User;
import com.aichatbot.util.CsrfUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Role-based access, session handling and CSRF checks, tested with fake request objects. */
class AccessControlFilterTest {

    private static User user(String role) {
        User u = new User();
        u.setId(1);
        u.setRole(role);
        return u;
    }

    private void runAuth(TestSupport.Recorder r) throws Exception {
        new AuthenticationFilter().doFilter(r.request(), r.response(), r.chain());
    }

    private void runAdmin(TestSupport.Recorder r) throws Exception {
        new AdminAuthorizationFilter().doFilter(r.request(), r.response(), r.chain());
    }

    private void runCsrf(TestSupport.Recorder r) throws Exception {
        new CsrfFilter().doFilter(r.request(), r.response(), r.chain());
    }

    @Test
    void visitorWithoutSessionIsRedirectedToLogin() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.hasSession = false;
        runAuth(r);
        assertEquals("/app/login", r.redirectedTo);
        assertFalse(r.chainCalled);
    }

    @Test
    void sessionWithoutLoggedInUserIsRedirectedToLogin() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        runAuth(r);
        assertEquals("/app/login", r.redirectedTo);
        assertFalse(r.chainCalled);
    }

    @Test
    void loggedInUserPassesAuthentication() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.sessionAttributes.put(AuthenticationFilter.SESSION_USER, user("USER"));
        runAuth(r);
        assertTrue(r.chainCalled);
        assertNull(r.redirectedTo);
    }

    @Test
    void ordinaryUserCannotOpenAdminPages() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.sessionAttributes.put(AuthenticationFilter.SESSION_USER, user("USER"));
        runAdmin(r);
        assertFalse(r.chainCalled);
        assertEquals(403, r.status);
        assertEquals("/WEB-INF/views/access-denied.jsp", r.forwardedTo);
    }

    @Test
    void adminCanOpenAdminPages() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.sessionAttributes.put(AuthenticationFilter.SESSION_USER, user("ADMIN"));
        runAdmin(r);
        assertTrue(r.chainCalled);
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.method = "POST";
        r.sessionAttributes.put(CsrfUtil.SESSION_KEY, "abc123");
        runCsrf(r);
        assertFalse(r.chainCalled);
        assertEquals(403, r.status);
    }

    @Test
    void postWithWrongCsrfTokenIsRejected() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.method = "POST";
        r.sessionAttributes.put(CsrfUtil.SESSION_KEY, "abc123");
        r.parameters.put(CsrfUtil.PARAM_NAME, "evil");
        runCsrf(r);
        assertFalse(r.chainCalled);
    }

    @Test
    void postWithMatchingCsrfTokenIsAllowed() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        r.method = "POST";
        r.sessionAttributes.put(CsrfUtil.SESSION_KEY, "abc123");
        r.parameters.put(CsrfUtil.PARAM_NAME, "abc123");
        runCsrf(r);
        assertTrue(r.chainCalled);
    }

    @Test
    void getRequestCreatesATokenInTheSession() throws Exception {
        TestSupport.Recorder r = new TestSupport.Recorder();
        runCsrf(r);
        assertTrue(r.chainCalled);
        assertNotNull(r.sessionAttributes.get(CsrfUtil.SESSION_KEY));
    }
}
