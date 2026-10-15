package com.aichatbot;

import com.aichatbot.dao.UserDAO;
import com.aichatbot.model.User;
import com.aichatbot.service.AuthService;
import com.aichatbot.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Registration, login and admin-bootstrap rules, using an in-memory fake instead of MySQL. */
class AuthServiceTest {

    static class FakeUserDAO extends UserDAO {
        final List<User> users = new ArrayList<>();

        @Override
        public User create(User user) {
            user.setId(users.size() + 1);
            users.add(user);
            return user;
        }

        @Override
        public User findByEmail(String email) {
            return users.stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
        }

        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email) != null;
        }

        @Override
        public int countAdmins() {
            return (int) users.stream().filter(User::isAdmin).count();
        }
    }

    private final FakeUserDAO dao = new FakeUserDAO();
    private final AuthService auth = new AuthService(dao);

    @Test
    void registrationStoresHashedPasswordAndForcesUserRole() {
        Map<String, String> errors = auth.register("Asha Rao", "Asha@Example.com", "Passw0rdOK", "Passw0rdOK");
        assertTrue(errors.isEmpty());
        User saved = dao.users.get(0);
        assertEquals("asha@example.com", saved.getEmail());
        assertEquals(User.ROLE_USER, saved.getRole());
        assertFalse(saved.getPasswordHash().contains("Passw0rdOK"));
        assertTrue(PasswordUtil.verify("Passw0rdOK", saved.getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        auth.register("A", "a@example.com", "Passw0rdOK", "Passw0rdOK");
        Map<String, String> errors = auth.register("B", "A@example.com", "Passw0rdOK", "Passw0rdOK");
        assertTrue(errors.containsKey("email"));
        assertEquals(1, dao.users.size());
    }

    @Test
    void invalidInputProducesFieldErrors() {
        Map<String, String> errors = auth.register("", "not-an-email", "short", "different");
        assertTrue(errors.containsKey("fullName"));
        assertTrue(errors.containsKey("email"));
        assertTrue(errors.containsKey("password"));
        assertTrue(dao.users.isEmpty());
    }

    @Test
    void passwordConfirmationMustMatch() {
        assertTrue(auth.register("A", "a@example.com", "Passw0rdOK", "Passw0rdNO").containsKey("confirmPassword"));
    }

    @Test
    void loginSucceedsWithCorrectPasswordAndHidesTheHash() {
        auth.register("A", "a@example.com", "Passw0rdOK", "Passw0rdOK");
        User user = auth.login(" A@example.com ", "Passw0rdOK");
        assertNotNull(user);
        assertNull(user.getPasswordHash());
    }

    @Test
    void loginFailsForWrongPasswordUnknownEmailAndEmptyInput() {
        auth.register("A", "a@example.com", "Passw0rdOK", "Passw0rdOK");
        assertNull(auth.login("a@example.com", "wrong-password1"));
        assertNull(auth.login("nobody@example.com", "Passw0rdOK"));
        assertNull(auth.login("", ""));
        assertNull(auth.login(null, null));
    }

    @Test
    void firstAdminIsCreatedOnlyOnce() {
        assertTrue(auth.createAdminIfMissing("Boss", "admin@example.com", "AdminPass1"));
        assertFalse(auth.createAdminIfMissing("Other", "other@example.com", "AdminPass2"));
        assertEquals(1, dao.users.size());
        assertTrue(dao.users.get(0).isAdmin());
        assertTrue(PasswordUtil.verify("AdminPass1", dao.users.get(0).getPasswordHash()));
    }
}
