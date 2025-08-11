package com.dcf.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    private PasswordUtil passwordUtil;

    @BeforeEach
    void setUp() {
        passwordUtil = new PasswordUtil();
    }

    @Test
    @DisplayName("Password hashing should work correctly")
    void testPasswordHashing() {
        String plainPassword = "TestPassword123";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.length() > plainPassword.length());
    }

    @Test
    @DisplayName("Password verification should work correctly")
    void testPasswordVerification() {
        String plainPassword = "TestPassword123";
        String hashedPassword = passwordUtil.hashPassword(plainPassword);

        assertTrue(passwordUtil.verifyPassword(plainPassword, hashedPassword));
        assertFalse(passwordUtil.verifyPassword("WrongPassword", hashedPassword));
    }

    @Test
    @DisplayName("Valid password should pass validation")
    void testValidPassword() {
        String validPassword = "TestPassword123";
        assertTrue(passwordUtil.isValidPassword(validPassword));
        assertNull(passwordUtil.getPasswordValidationError(validPassword));
    }

    @Test
    @DisplayName("Short password should fail validation")
    void testShortPassword() {
        String shortPassword = "Test1";
        assertFalse(passwordUtil.isValidPassword(shortPassword));
        assertEquals("Password must be at least 8 characters long", 
                    passwordUtil.getPasswordValidationError(shortPassword));
    }

    @Test
    @DisplayName("Password without digit should fail validation")
    void testPasswordWithoutDigit() {
        String passwordWithoutDigit = "TestPassword";
        assertFalse(passwordUtil.isValidPassword(passwordWithoutDigit));
        assertEquals("Password must contain at least one digit", 
                    passwordUtil.getPasswordValidationError(passwordWithoutDigit));
    }

    @Test
    @DisplayName("Password without lowercase should fail validation")
    void testPasswordWithoutLowercase() {
        String passwordWithoutLower = "TESTPASSWORD123";
        assertFalse(passwordUtil.isValidPassword(passwordWithoutLower));
        assertEquals("Password must contain at least one lowercase letter", 
                    passwordUtil.getPasswordValidationError(passwordWithoutLower));
    }

    @Test
    @DisplayName("Password without uppercase should fail validation")
    void testPasswordWithoutUppercase() {
        String passwordWithoutUpper = "testpassword123";
        assertFalse(passwordUtil.isValidPassword(passwordWithoutUpper));
        assertEquals("Password must contain at least one uppercase letter", 
                    passwordUtil.getPasswordValidationError(passwordWithoutUpper));
    }

    @Test
    @DisplayName("Null password should fail validation")
    void testNullPassword() {
        assertFalse(passwordUtil.isValidPassword(null));
        assertEquals("Password is required", 
                    passwordUtil.getPasswordValidationError(null));
    }

    @Test
    @DisplayName("Empty password should fail validation")
    void testEmptyPassword() {
        String emptyPassword = "";
        assertFalse(passwordUtil.isValidPassword(emptyPassword));
        assertEquals("Password is required", 
                    passwordUtil.getPasswordValidationError(emptyPassword));
    }

    @Test
    @DisplayName("Different hash calls should produce different results")
    void testHashingConsistency() {
        String plainPassword = "TestPassword123";
        String hash1 = passwordUtil.hashPassword(plainPassword);
        String hash2 = passwordUtil.hashPassword(plainPassword);

        // BCrypt should produce different hashes each time due to salt
        assertNotEquals(hash1, hash2);
        
        // But both should verify correctly
        assertTrue(passwordUtil.verifyPassword(plainPassword, hash1));
        assertTrue(passwordUtil.verifyPassword(plainPassword, hash2));
    }
}