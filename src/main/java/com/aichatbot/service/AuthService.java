package com.aichatbot.service;

import com.aichatbot.dao.UserDAO;
import com.aichatbot.exception.DatabaseException;
import com.aichatbot.model.User;
import com.aichatbot.util.PasswordUtil;
import com.aichatbot.util.Validator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registration, login and account management rules. */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers an ordinary USER. The role is never taken from the request, so nobody can
     * register as an admin.
     *
     * @return field -> error message; an empty map means the account was created
     */
    public Map<String, String> register(String fullName, String email, String password, String confirmPassword) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateName(fullName, errors);
        if (!Validator.isValidEmail(email)) {
            errors.put("email", "Enter a valid email address.");
        }
        String passwordProblem = Validator.passwordProblem(password);
        if (passwordProblem != null) {
            errors.put("password", passwordProblem);
        } else if (!password.equals(confirmPassword)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (!errors.isEmpty()) {
            return errors;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userDAO.existsByEmail(normalizedEmail)) {
            errors.put("email", "An account with this email already exists.");
            return errors;
        }
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(User.ROLE_USER);
        try {
            userDAO.create(user);
        } catch (DatabaseException e) {
            if (e.isDuplicateKey()) { // two people registering the same email at the same moment
                errors.put("email", "An account with this email already exists.");
            } else {
                throw e;
            }
        }
        return errors;
    }

    /** @return the user (password hash removed) or null when the email/password is wrong */
    public User login(String email, String password) {
        if (Validator.isBlank(email) || password == null || password.isEmpty()) {
            return null;
        }
        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            return null; // same result for unknown email and wrong password
        }
        user.setPasswordHash(null);
        return user;
    }

    public User findUser(int id) {
        User user = userDAO.findById(id);
        if (user != null) {
            user.setPasswordHash(null);
        }
        return user;
    }

    public Map<String, String> updateProfile(int userId, String fullName) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateName(fullName, errors);
        if (errors.isEmpty()) {
            userDAO.updateName(userId, fullName.trim());
        }
        return errors;
    }

    public Map<String, String> changePassword(int userId, String current, String newPassword, String confirm) {
        Map<String, String> errors = new LinkedHashMap<>();
        User user = userDAO.findById(userId);
        if (user == null || !PasswordUtil.verify(current, user.getPasswordHash())) {
            errors.put("currentPassword", "Current password is incorrect.");
        }
        String problem = Validator.passwordProblem(newPassword);
        if (problem != null) {
            errors.put("newPassword", problem);
        } else if (!newPassword.equals(confirm)) {
            errors.put("confirmPassword", "Passwords do not match.");
        }
        if (errors.isEmpty()) {
            userDAO.updatePassword(userId, PasswordUtil.hash(newPassword));
        }
        return errors;
    }

    public List<User> listUsers() {
        List<User> users = userDAO.findAll();
        users.forEach(u -> u.setPasswordHash(null));
        return users;
    }

    /** Admin accounts cannot be deleted through the application. */
    public boolean deleteUser(int userId) {
        return userDAO.deleteUser(userId);
    }

    /**
     * Used once at startup (see AppContextListener): creates the first admin only if none exists.
     *
     * @return true when an admin account was created
     */
    public boolean createAdminIfMissing(String fullName, String email, String password) {
        if (userDAO.countAdmins() > 0) {
            return false;
        }
        if (!Validator.isValidEmail(email) || Validator.passwordProblem(password) != null) {
            throw new IllegalArgumentException("ADMIN_EMAIL must be a valid email and ADMIN_PASSWORD at least "
                    + "8 characters with a letter and a digit.");
        }
        User admin = new User();
        admin.setFullName(Validator.isBlank(fullName) ? "Administrator" : fullName.trim());
        admin.setEmail(email.trim().toLowerCase());
        admin.setPasswordHash(PasswordUtil.hash(password));
        admin.setRole(User.ROLE_ADMIN);
        userDAO.create(admin);
        return true;
    }

    private void validateName(String fullName, Map<String, String> errors) {
        if (Validator.isBlank(fullName)) {
            errors.put("fullName", "Full name is required.");
        } else if (fullName.trim().length() > 100) {
            errors.put("fullName", "Full name must be at most 100 characters.");
        }
    }
}
