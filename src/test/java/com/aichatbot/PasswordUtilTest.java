package com.aichatbot;

import com.aichatbot.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashIsNeverThePlainPassword() {
        String hash = PasswordUtil.hash("Secret123");
        assertNotEquals("Secret123", hash);
        assertFalse(hash.contains("Secret123"));
    }

    @Test
    void correctPasswordVerifiesAndWrongPasswordFails() {
        String hash = PasswordUtil.hash("Secret123");
        assertTrue(PasswordUtil.verify("Secret123", hash));
        assertFalse(PasswordUtil.verify("secret123", hash));
        assertFalse(PasswordUtil.verify("Secret123", "not-a-valid-hash"));
    }

    @Test
    void samePasswordGetsDifferentHashesBecauseOfSalt() {
        assertNotEquals(PasswordUtil.hash("Secret123"), PasswordUtil.hash("Secret123"));
    }
}
