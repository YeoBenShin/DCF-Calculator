package com.dcf.controller;

import com.dcf.dto.AuthRequest;
import com.dcf.dto.AuthResponse;
import com.dcf.service.AuthenticationService;
import com.dcf.service.AuthenticationService.AuthenticationException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationService authenticationService;

    /**
     * User registration endpoint
     * @param authRequest the registration request containing email and password
     * @return AuthResponse with JWT token and user information
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Registration attempt for email: {}", authRequest.getEmail());
        
        try {
            AuthResponse response = authenticationService.register(authRequest);
            logger.info("User registration successful for email: {}", authRequest.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (AuthenticationException e) {
            logger.warn("Registration failed for email {}: {}", authRequest.getEmail(), e.getMessage());
            AuthResponse errorResponse = new AuthResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during registration for email {}: {}", authRequest.getEmail(), e.getMessage(), e);
            AuthResponse errorResponse = new AuthResponse("Registration failed due to server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * User login endpoint
     * @param authRequest the login request containing email and password
     * @return AuthResponse with JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        logger.info("Login attempt for email: {}", authRequest.getEmail());
        
        try {
            AuthResponse response = authenticationService.login(authRequest);
            logger.info("User login successful for email: {}", authRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("Login failed for email {}: {}", authRequest.getEmail(), e.getMessage());
            AuthResponse errorResponse = new AuthResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during login for email {}: {}", authRequest.getEmail(), e.getMessage(), e);
            AuthResponse errorResponse = new AuthResponse("Login failed due to server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Token refresh endpoint
     * @param refreshRequest containing the old token
     * @return new JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshRequest) {
        logger.info("Token refresh attempt");
        
        try {
            String newToken = authenticationService.refreshToken(refreshRequest.getToken());
            AuthResponse response = new AuthResponse();
            response.setToken(newToken);
            response.setMessage("Token refreshed successfully");
            
            logger.info("Token refresh successful");
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            AuthResponse errorResponse = new AuthResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh: {}", e.getMessage(), e);
            AuthResponse errorResponse = new AuthResponse("Token refresh failed due to server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Token validation endpoint
     * @param token the JWT token to validate
     * @return validation status
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest tokenRequest) {
        logger.info("Token validation attempt");
        
        try {
            String userId = authenticationService.validateTokenAndGetUserId(tokenRequest.getToken());
            TokenValidationResponse response = new TokenValidationResponse(true, "Token is valid", userId);
            
            logger.info("Token validation successful for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            TokenValidationResponse response = new TokenValidationResponse(false, e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            logger.error("Unexpected error during token validation: {}", e.getMessage(), e);
            TokenValidationResponse response = new TokenValidationResponse(false, "Token validation failed due to server error", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * DTO for refresh token requests
     */
    public static class RefreshTokenRequest {
        private String token;

        public RefreshTokenRequest() {}

        public RefreshTokenRequest(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * DTO for token validation requests
     */
    public static class TokenValidationRequest {
        private String token;

        public TokenValidationRequest() {}

        public TokenValidationRequest(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * DTO for token validation responses
     */
    public static class TokenValidationResponse {
        private boolean valid;
        private String message;
        private String userId;

        public TokenValidationResponse() {}

        public TokenValidationResponse(boolean valid, String message, String userId) {
            this.valid = valid;
            this.message = message;
            this.userId = userId;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}