package com.dcf.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    private Validator validator;
    private User user;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        user = new User();
    }

    @Test
    @DisplayName("Valid user should pass validation")
    void testValidUser() {
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword123");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("User with invalid email should fail validation")
    void testInvalidEmail() {
        user.setEmail("invalid-email");
        user.setPasswordHash("hashedPassword123");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        
        boolean emailViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Email should be valid"));
        assertTrue(emailViolationFound);
    }

    @Test
    @DisplayName("User with blank email should fail validation")
    void testBlankEmail() {
        user.setEmail("");
        user.setPasswordHash("hashedPassword123");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        
        boolean emailViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Email is required"));
        assertTrue(emailViolationFound);
    }

    @Test
    @DisplayName("User with blank password should fail validation")
    void testBlankPassword() {
        user.setEmail("test@example.com");
        user.setPasswordHash("");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        
        boolean passwordViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password is required"));
        assertTrue(passwordViolationFound);
    }

    @Test
    @DisplayName("User with short password should fail validation")
    void testShortPassword() {
        user.setEmail("test@example.com");
        user.setPasswordHash("short");

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        
        boolean passwordViolationFound = violations.stream()
            .anyMatch(v -> v.getMessage().contains("Password must be at least 8 characters long"));
        assertTrue(passwordViolationFound);
    }

    @Test
    @DisplayName("Watchlist operations should work correctly")
    void testWatchlistOperations() {
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword123");

        // Test adding to watchlist
        user.addToWatchlist("AAPL");
        assertTrue(user.isInWatchlist("AAPL"));
        assertEquals(1, user.getWatchlist().size());

        // Test adding duplicate (should not add)
        user.addToWatchlist("aapl"); // lowercase should be converted to uppercase
        assertEquals(1, user.getWatchlist().size());

        // Test adding another ticker
        user.addToWatchlist("GOOGL");
        assertTrue(user.isInWatchlist("GOOGL"));
        assertEquals(2, user.getWatchlist().size());

        // Test removing from watchlist
        user.removeFromWatchlist("AAPL");
        assertFalse(user.isInWatchlist("AAPL"));
        assertEquals(1, user.getWatchlist().size());
    }

    @Test
    @DisplayName("User creation should set timestamps")
    void testUserCreationTimestamps() {
        User newUser = new User("test@example.com", "hashedPassword123");
        
        assertNotNull(newUser.getCreatedAt());
        assertNotNull(newUser.getUpdatedAt());
        assertEquals(newUser.getCreatedAt(), newUser.getUpdatedAt());
    }

    @Test
    @DisplayName("Watchlist should handle case insensitive tickers")
    void testWatchlistCaseInsensitive() {
        user.addToWatchlist("aapl");
        assertTrue(user.isInWatchlist("AAPL"));
        assertTrue(user.isInWatchlist("aapl"));
        
        // Should contain uppercase version
        assertTrue(user.getWatchlist().contains("AAPL"));
        assertFalse(user.getWatchlist().contains("aapl"));
    }
}