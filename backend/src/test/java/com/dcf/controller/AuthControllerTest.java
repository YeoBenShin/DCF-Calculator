package com.dcf.controller;

import com.dcf.dto.AuthRequest;
import com.dcf.dto.AuthResponse;
import com.dcf.dto.UserDto;
import com.dcf.service.AuthenticationService;
import com.dcf.service.AuthenticationService.AuthenticationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthRequest validAuthRequest;
    private AuthResponse successResponse;

    @BeforeEach
    void setUp() {
        validAuthRequest = new AuthRequest("test@example.com", "Password123!");
        
        UserDto userDto = new UserDto("user123", "test@example.com", new ArrayList<>());
        successResponse = new AuthResponse("jwt-token-here", userDto);
    }

    @Test
    @DisplayName("POST /auth/signup - Should register user successfully")
    void testSignupSuccess() throws Exception {
        // Arrange
        when(authenticationService.register(any(AuthRequest.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.user.userId").value("user123"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        verify(authenticationService).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/signup - Should return 400 for duplicate email")
    void testSignupDuplicateEmail() throws Exception {
        // Arrange
        when(authenticationService.register(any(AuthRequest.class)))
                .thenThrow(new AuthenticationException("User with email test@example.com already exists"));

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User with email test@example.com already exists"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        verify(authenticationService).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/signup - Should return 400 for invalid email")
    void testSignupInvalidEmail() throws Exception {
        // Arrange
        AuthRequest invalidRequest = new AuthRequest("invalid-email", "Password123!");

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/signup - Should return 400 for weak password")
    void testSignupWeakPassword() throws Exception {
        // Arrange
        AuthRequest weakPasswordRequest = new AuthRequest("test@example.com", "weak");

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(weakPasswordRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/signup - Should return 500 for server error")
    void testSignupServerError() throws Exception {
        // Arrange
        when(authenticationService.register(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Registration failed due to server error"));

        verify(authenticationService).register(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should login user successfully")
    void testLoginSuccess() throws Exception {
        // Arrange
        when(authenticationService.login(any(AuthRequest.class))).thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.user.userId").value("user123"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        verify(authenticationService).login(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 401 for invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        when(authenticationService.login(any(AuthRequest.class)))
                .thenThrow(new AuthenticationException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validAuthRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        verify(authenticationService).login(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 for missing email")
    void testLoginMissingEmail() throws Exception {
        // Arrange
        AuthRequest invalidRequest = new AuthRequest("", "Password123!");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 for missing password")
    void testLoginMissingPassword() throws Exception {
        // Arrange
        AuthRequest invalidRequest = new AuthRequest("test@example.com", "");

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).login(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /auth/refresh - Should refresh token successfully")
    void testRefreshTokenSuccess() throws Exception {
        // Arrange
        String oldToken = "old-jwt-token";
        String newToken = "new-jwt-token";
        AuthController.RefreshTokenRequest refreshRequest = new AuthController.RefreshTokenRequest(oldToken);
        
        when(authenticationService.refreshToken(oldToken)).thenReturn(newToken);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(newToken))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));

        verify(authenticationService).refreshToken(oldToken);
    }

    @Test
    @DisplayName("POST /auth/refresh - Should return 401 for invalid token")
    void testRefreshTokenInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";
        AuthController.RefreshTokenRequest refreshRequest = new AuthController.RefreshTokenRequest(invalidToken);
        
        when(authenticationService.refreshToken(invalidToken))
                .thenThrow(new AuthenticationException("Invalid or expired token"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));

        verify(authenticationService).refreshToken(invalidToken);
    }

    @Test
    @DisplayName("POST /auth/validate - Should validate token successfully")
    void testValidateTokenSuccess() throws Exception {
        // Arrange
        String validToken = "valid-jwt-token";
        String userId = "user123";
        AuthController.TokenValidationRequest validationRequest = new AuthController.TokenValidationRequest(validToken);
        
        when(authenticationService.validateTokenAndGetUserId(validToken)).thenReturn(userId);

        // Act & Assert
        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.userId").value(userId));

        verify(authenticationService).validateTokenAndGetUserId(validToken);
    }

    @Test
    @DisplayName("POST /auth/validate - Should return 401 for invalid token")
    void testValidateTokenInvalid() throws Exception {
        // Arrange
        String invalidToken = "invalid-token";
        AuthController.TokenValidationRequest validationRequest = new AuthController.TokenValidationRequest(invalidToken);
        
        when(authenticationService.validateTokenAndGetUserId(invalidToken))
                .thenThrow(new AuthenticationException("Invalid or expired token"));

        // Act & Assert
        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                .andExpect(jsonPath("$.userId").doesNotExist());

        verify(authenticationService).validateTokenAndGetUserId(invalidToken);
    }
}