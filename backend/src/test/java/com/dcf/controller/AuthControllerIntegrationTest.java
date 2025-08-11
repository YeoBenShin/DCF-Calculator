package com.dcf.controller;

import com.dcf.dto.AuthRequest;
import com.dcf.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Complete signup and login flow")
    void testCompleteAuthFlow() throws Exception {
        AuthRequest authRequest = new AuthRequest("integration@example.com", "Password123!");

        // Test signup
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));

        // Test login with same credentials
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("integration@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("Integration Test: Duplicate signup should fail")
    void testDuplicateSignup() throws Exception {
        AuthRequest authRequest = new AuthRequest("duplicate@example.com", "Password123!");

        // First signup should succeed
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated());

        // Second signup with same email should fail
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User with email duplicate@example.com already exists"));
    }

    @Test
    @DisplayName("Integration Test: Login with wrong password should fail")
    void testLoginWrongPassword() throws Exception {
        AuthRequest signupRequest = new AuthRequest("wrongpass@example.com", "Password123!");
        AuthRequest loginRequest = new AuthRequest("wrongpass@example.com", "WrongPassword!");

        // Signup first
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());

        // Login with wrong password should fail
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Integration Test: Login with non-existent user should fail")
    void testLoginNonExistentUser() throws Exception {
        AuthRequest loginRequest = new AuthRequest("nonexistent@example.com", "Password123!");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Integration Test: Token validation flow")
    void testTokenValidationFlow() throws Exception {
        AuthRequest authRequest = new AuthRequest("tokentest@example.com", "Password123!");

        // Signup and get token
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from response
        String token = objectMapper.readTree(signupResponse).get("token").asText();

        // Validate token
        AuthController.TokenValidationRequest validationRequest = new AuthController.TokenValidationRequest(token);
        mockMvc.perform(post("/auth/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"))
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    @DisplayName("Integration Test: Token refresh flow")
    void testTokenRefreshFlow() throws Exception {
        AuthRequest authRequest = new AuthRequest("refreshtest@example.com", "Password123!");

        // Signup and get token
        String signupResponse = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from response
        String originalToken = objectMapper.readTree(signupResponse).get("token").asText();

        // Refresh token
        AuthController.RefreshTokenRequest refreshRequest = new AuthController.RefreshTokenRequest(originalToken);
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"));
    }
}