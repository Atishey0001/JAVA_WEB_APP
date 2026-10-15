package com.aichatbot.util;

import java.util.regex.Pattern;

/** Small reusable input-validation helpers (server-side validation). */
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validator() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && email.trim().length() <= 150 && EMAIL.matcher(email.trim()).matches();
    }

    /** @return an error message, or null when the password is acceptable. */
    public static String passwordProblem(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (password.length() > 72) {
            return "Password must be at most 72 characters.";
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) {
            return "Password must contain at least one letter and one digit.";
        }
        return null;
    }
}
