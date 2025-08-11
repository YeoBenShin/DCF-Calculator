package com.dcf.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class EmailUtilTest {

    private EmailUtil emailUtil;

    @BeforeEach
    void setUp() {
        emailUtil = new EmailUtil();
    }

    @Test
    @DisplayName("Valid email should pass validation")
    void testValidEmail() {
        String validEmail = "test@example.com";
        assertTrue(emailUtil.isValidEmail(validEmail));
        assertNull(emailUtil.getEmailValidationError(validEmail));
    }

    @Test
    @DisplayName("Valid email with subdomain should pass validation")
    void testValidEmailWithSubdomain() {
        String validEmail = "user@mail.example.com";
        assertTrue(emailUtil.isValidEmail(validEmail));
        assertNull(emailUtil.getEmailValidationError(validEmail));
    }

    @Test
    @DisplayName("Valid email with numbers should pass validation")
    void testValidEmailWithNumbers() {
        String validEmail = "user123@example123.com";
        assertTrue(emailUtil.isValidEmail(validEmail));
        assertNull(emailUtil.getEmailValidationError(validEmail));
    }

    @Test
    @DisplayName("Valid email with special characters should pass validation")
    void testValidEmailWithSpecialChars() {
        String validEmail = "user.name+tag@example.com";
        assertTrue(emailUtil.isValidEmail(validEmail));
        assertNull(emailUtil.getEmailValidationError(validEmail));
    }

    @Test
    @DisplayName("Invalid email without @ should fail validation")
    void testInvalidEmailWithoutAt() {
        String invalidEmail = "testexample.com";
        assertFalse(emailUtil.isValidEmail(invalidEmail));
        assertEquals("Please enter a valid email address", 
                    emailUtil.getEmailValidationError(invalidEmail));
    }

    @Test
    @DisplayName("Invalid email without domain should fail validation")
    void testInvalidEmailWithoutDomain() {
        String invalidEmail = "test@";
        assertFalse(emailUtil.isValidEmail(invalidEmail));
        assertEquals("Please enter a valid email address", 
                    emailUtil.getEmailValidationError(invalidEmail));
    }

    @Test
    @DisplayName("Invalid email without local part should fail validation")
    void testInvalidEmailWithoutLocalPart() {
        String invalidEmail = "@example.com";
        assertFalse(emailUtil.isValidEmail(invalidEmail));
        assertEquals("Please enter a valid email address", 
                    emailUtil.getEmailValidationError(invalidEmail));
    }

    @Test
    @DisplayName("Invalid email with invalid domain should fail validation")
    void testInvalidEmailWithInvalidDomain() {
        String invalidEmail = "test@example";
        assertFalse(emailUtil.isValidEmail(invalidEmail));
        assertEquals("Please enter a valid email address", 
                    emailUtil.getEmailValidationError(invalidEmail));
    }

    @Test
    @DisplayName("Null email should fail validation")
    void testNullEmail() {
        assertFalse(emailUtil.isValidEmail(null));
        assertEquals("Email is required", 
                    emailUtil.getEmailValidationError(null));
    }

    @Test
    @DisplayName("Empty email should fail validation")
    void testEmptyEmail() {
        String emptyEmail = "";
        assertFalse(emailUtil.isValidEmail(emptyEmail));
        assertEquals("Email is required", 
                    emailUtil.getEmailValidationError(emptyEmail));
    }

    @Test
    @DisplayName("Email normalization should work correctly")
    void testEmailNormalization() {
        String email = "  Test@Example.COM  ";
        String normalizedEmail = emailUtil.normalizeEmail(email);
        assertEquals("test@example.com", normalizedEmail);
    }

    @Test
    @DisplayName("Email normalization should handle null")
    void testEmailNormalizationWithNull() {
        assertNull(emailUtil.normalizeEmail(null));
    }

    @Test
    @DisplayName("Email normalization should handle empty string")
    void testEmailNormalizationWithEmpty() {
        String emptyEmail = "";
        String normalizedEmail = emailUtil.normalizeEmail(emptyEmail);
        assertEquals("", normalizedEmail);
    }

    @Test
    @DisplayName("Email normalization should handle whitespace only")
    void testEmailNormalizationWithWhitespace() {
        String whitespaceEmail = "   ";
        String normalizedEmail = emailUtil.normalizeEmail(whitespaceEmail);
        assertEquals("", normalizedEmail);
    }
}