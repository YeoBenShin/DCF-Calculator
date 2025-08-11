package com.dcf.service;

import com.dcf.dto.AuthRequest;
import com.dcf.dto.AuthResponse;
import com.dcf.dto.UserDto;
import com.dcf.entity.User;
import com.dcf.repository.UserRepository;
import com.dcf.service.AuthenticationService.AuthenticationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User mockUser;
    private AuthRequest validAuthRequest;

    @BeforeEach
    void setUp() {
        // Initialize AuthenticationService with test values
        authenticationService = new AuthenticationService("testSecretKeyForJWTThatIsLongEnough", 86400000L);
        ReflectionTestUtils.setField(authenticationService, "userRepository", userRepository);

        mockUser = new User("test@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMye");
        mockUser.setUserId("user123");

        validAuthRequest = new AuthRequest("test@example.com", "Password123!");
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterSuccess() throws AuthenticationException {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        AuthResponse response = authenticationService.register(validAuthRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("user123", response.getUser().getUserId());
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user already exists")
    void testRegisterUserAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(validAuthRequest));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for weak password")
    void testRegisterWeakPassword() {
        // Arrange
        AuthRequest weakPasswordRequest = new AuthRequest("test@example.com", "weak");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(weakPasswordRequest));
        
        assertTrue(exception.getMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Should throw exception for password without digit")
    void testRegisterPasswordWithoutDigit() {
        // Arrange
        AuthRequest noDigitRequest = new AuthRequest("test@example.com", "Password!");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(noDigitRequest));
        
        assertTrue(exception.getMessage().contains("at least one digit"));
    }

    @Test
    @DisplayName("Should throw exception for password without letter")
    void testRegisterPasswordWithoutLetter() {
        // Arrange
        AuthRequest noLetterRequest = new AuthRequest("test@example.com", "123456789!");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(noLetterRequest));
        
        assertTrue(exception.getMessage().contains("at least one letter"));
    }

    @Test
    @DisplayName("Should throw exception for password without special character")
    void testRegisterPasswordWithoutSpecialChar() {
        // Arrange
        AuthRequest noSpecialCharRequest = new AuthRequest("test@example.com", "Password123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(noSpecialCharRequest));
        
        assertTrue(exception.getMessage().contains("at least one special character"));
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLoginSuccess() throws AuthenticationException {
        // Arrange
        // Create a user with properly hashed password
        User userWithHashedPassword = new User("test@example.com", 
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIX0wVaU6l4gMY.qw1lkRMppSiB1P2HO"); // "Password123!" hashed
        userWithHashedPassword.setUserId("user123");
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userWithHashedPassword));

        // Act
        AuthResponse response = authenticationService.login(validAuthRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUser());
        assertEquals("test@example.com", response.getUser().getEmail());
        assertEquals("user123", response.getUser().getUserId());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception for non-existent user")
    void testLoginUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.login(validAuthRequest));
        
        assertTrue(exception.getMessage().contains("Invalid email or password"));
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void testLoginWrongPassword() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.login(validAuthRequest));
        
        assertTrue(exception.getMessage().contains("Invalid email or password"));
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void testGenerateToken() {
        // Act
        String token = authenticationService.generateToken(mockUser);

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // Verify token contains user information
        String extractedUserId = authenticationService.extractUserIdFromToken(token);
        assertEquals("user123", extractedUserId);
    }

    @Test
    @DisplayName("Should validate JWT token and extract user ID")
    void testValidateTokenAndGetUserId() throws AuthenticationException {
        // Arrange
        String token = authenticationService.generateToken(mockUser);

        // Act
        String userId = authenticationService.validateTokenAndGetUserId(token);

        // Assert
        assertEquals("user123", userId);
    }

    @Test
    @DisplayName("Should throw exception for invalid JWT token")
    void testValidateInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.validateTokenAndGetUserId(invalidToken));
        
        assertTrue(exception.getMessage().contains("Invalid or expired token"));
    }

    @Test
    @DisplayName("Should extract user ID from valid token")
    void testExtractUserIdFromToken() {
        // Arrange
        String token = authenticationService.generateToken(mockUser);

        // Act
        String userId = authenticationService.extractUserIdFromToken(token);

        // Assert
        assertEquals("user123", userId);
    }

    @Test
    @DisplayName("Should return null for invalid token when extracting user ID")
    void testExtractUserIdFromInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        String userId = authenticationService.extractUserIdFromToken(invalidToken);

        // Assert
        assertNull(userId);
    }

    @Test
    @DisplayName("Should detect non-expired token")
    void testIsTokenExpiredFalse() {
        // Arrange
        String token = authenticationService.generateToken(mockUser);

        // Act
        boolean isExpired = authenticationService.isTokenExpired(token);

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should detect invalid token as expired")
    void testIsTokenExpiredInvalidToken() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean isExpired = authenticationService.isTokenExpired(invalidToken);

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should refresh JWT token successfully")
    void testRefreshToken() throws AuthenticationException {
        // Arrange
        String oldToken = authenticationService.generateToken(mockUser);
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        String newToken = authenticationService.refreshToken(oldToken);

        // Assert
        assertNotNull(newToken);
        assertNotEquals(oldToken, newToken);
        
        // Verify new token is valid
        String userId = authenticationService.validateTokenAndGetUserId(newToken);
        assertEquals("user123", userId);
    }

    @Test
    @DisplayName("Should throw exception when refreshing token for non-existent user")
    void testRefreshTokenUserNotFound() {
        // Arrange
        String token = authenticationService.generateToken(mockUser);
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.refreshToken(token));
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Should get user from valid token")
    void testGetUserFromToken() throws AuthenticationException {
        // Arrange
        String token = authenticationService.generateToken(mockUser);
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act
        UserDto userDto = authenticationService.getUserFromToken(token);

        // Assert
        assertNotNull(userDto);
        assertEquals("user123", userDto.getUserId());
        assertEquals("test@example.com", userDto.getEmail());
    }

    @Test
    @DisplayName("Should change password successfully")
    void testChangePasswordSuccess() throws AuthenticationException {
        // Arrange
        User userWithHashedPassword = new User("test@example.com", 
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIX0wVaU6l4gMY.qw1lkRMppSiB1P2HO"); // "Password123!" hashed
        userWithHashedPassword.setUserId("user123");
        
        when(userRepository.findById("user123")).thenReturn(Optional.of(userWithHashedPassword));
        when(userRepository.save(any(User.class))).thenReturn(userWithHashedPassword);

        // Act
        authenticationService.changePassword("user123", "Password123!", "NewPassword456!");

        // Assert
        verify(userRepository).findById("user123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception for wrong current password")
    void testChangePasswordWrongCurrentPassword() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(mockUser));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.changePassword("user123", "WrongPassword!", "NewPassword456!"));
        
        assertTrue(exception.getMessage().contains("Current password is incorrect"));
    }

    @Test
    @DisplayName("Should throw exception for weak new password")
    void testChangePasswordWeakNewPassword() {
        // Arrange
        User userWithHashedPassword = new User("test@example.com", 
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIX0wVaU6l4gMY.qw1lkRMppSiB1P2HO"); // "Password123!" hashed
        userWithHashedPassword.setUserId("user123");
        
        when(userRepository.findById("user123")).thenReturn(Optional.of(userWithHashedPassword));

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.changePassword("user123", "Password123!", "weak"));
        
        assertTrue(exception.getMessage().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Should throw exception when changing password for non-existent user")
    void testChangePasswordUserNotFound() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.changePassword("user123", "Password123!", "NewPassword456!"));
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void testRepositoryExceptions() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, 
            () -> authenticationService.register(validAuthRequest));
    }

    @Test
    @DisplayName("Should validate various password requirements")
    void testPasswordValidationEdgeCases() {
        // Test null password
        AuthRequest nullPasswordRequest = new AuthRequest("test@example.com", null);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        
        AuthenticationException exception = assertThrows(AuthenticationException.class,
            () -> authenticationService.register(nullPasswordRequest));
        assertTrue(exception.getMessage().contains("at least 8 characters"));

        // Test password with all requirements
        AuthRequest validPasswordRequest = new AuthRequest("test@example.com", "ValidPass123!");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        
        assertDoesNotThrow(() -> authenticationService.register(validPasswordRequest));
    }
}