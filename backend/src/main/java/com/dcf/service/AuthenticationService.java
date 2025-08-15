package com.dcf.service;

import com.dcf.dto.AuthRequest;
import com.dcf.dto.AuthResponse;
import com.dcf.dto.UserDto;
import com.dcf.entity.User;
import com.dcf.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final SecretKey jwtSecret;
    private final long jwtExpirationMs;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public AuthenticationService(
            @Value("${JWT_SECRET:defaultSecretKeyThatShouldBeChangedInProduction}") String jwtSecretString,
            @Value("${app.jwt.expiration:3600}") long jwtExpirationMs) {
        this.jwtSecret = Keys.hmacShaKeyFor(jwtSecretString.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new user
     * @param authRequest the registration request containing email and password
     * @return AuthResponse with JWT token and user information
     * @throws AuthenticationException if registration fails
     */
    public AuthResponse register(AuthRequest authRequest) throws AuthenticationException {
        logger.info("Attempting to register user with email: {}", authRequest.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(authRequest.getEmail())) {
            throw new AuthenticationException("User with email " + authRequest.getEmail() + " already exists");
        }

        // Validate password strength
        validatePassword(authRequest.getPassword());

        // Hash password
        String hashedPassword = passwordEncoder.encode(authRequest.getPassword());

        // Create new user
        User user = new User(authRequest.getEmail(), hashedPassword);
        user = userRepository.save(user);

        // Generate JWT token
        String token = generateToken(user);

        // Create user DTO
        UserDto userDto = new UserDto(user.getUserId(), user.getEmail(), user.getWatchlist());

        logger.info("Successfully registered user with ID: {}", user.getUserId());
        return new AuthResponse(token, userDto);
    }

    /**
     * Authenticate user login
     * @param authRequest the login request containing email and password
     * @return AuthResponse with JWT token and user information
     * @throws AuthenticationException if authentication fails
     */
    public AuthResponse login(AuthRequest authRequest) throws AuthenticationException {
        logger.info("Attempting to authenticate user with email: {}", authRequest.getEmail());

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(authRequest.getEmail());
        if (!userOptional.isPresent()) {
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userOptional.get();

        // Verify password
        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        // Generate JWT token
        String token = generateToken(user);

        // Create user DTO
        UserDto userDto = new UserDto(user.getUserId(), user.getEmail(), user.getWatchlist());

        logger.info("Successfully authenticated user with ID: {}", user.getUserId());
        return new AuthResponse(token, userDto);
    }

    /**
     * Generate JWT token for user
     * @param user the user to generate token for
     * @return JWT token string
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(user.getUserId())
                .claim("email", user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecret, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validate JWT token and extract user ID
     * @param token the JWT token to validate
     * @return user ID if token is valid
     * @throws AuthenticationException if token is invalid
     */
    public String validateTokenAndGetUserId(String token) throws AuthenticationException {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            throw new AuthenticationException("Invalid or expired token");
        }
    }

    /**
     * Extract user ID from JWT token without validation (for internal use)
     * @param token the JWT token
     * @return user ID if extractable, null otherwise
     */
    public String extractUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Could not extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if JWT token is expired
     * @param token the JWT token to check
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(jwtSecret)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // Consider invalid tokens as expired
        }
    }

    /**
     * Refresh JWT token for user
     * @param oldToken the existing JWT token
     * @return new JWT token
     * @throws AuthenticationException if old token is invalid or user not found
     */
    public String refreshToken(String oldToken) throws AuthenticationException {
        String userId = validateTokenAndGetUserId(oldToken);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new AuthenticationException("User not found");
        }

        return generateToken(userOptional.get());
    }

    /**
     * Get user information from JWT token
     * @param token the JWT token
     * @return UserDto with user information
     * @throws AuthenticationException if token is invalid or user not found
     */
    public UserDto getUserFromToken(String token) throws AuthenticationException {
        String userId = validateTokenAndGetUserId(token);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOptional.get();
        return new UserDto(user.getUserId(), user.getEmail(), user.getWatchlist());
    }

    /**
     * Change user password
     * @param userId the user ID
     * @param currentPassword the current password
     * @param newPassword the new password
     * @throws AuthenticationException if current password is wrong or validation fails
     */
    public void changePassword(String userId, String currentPassword, String newPassword) throws AuthenticationException {
        logger.info("Attempting to change password for user: {}", userId);

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new AuthenticationException("User not found");
        }

        User user = userOptional.get();

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Validate new password
        validatePassword(newPassword);

        // Hash and save new password
        String hashedNewPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedNewPassword);
        userRepository.save(user);

        logger.info("Successfully changed password for user: {}", userId);
    }

    /**
     * Validate password strength
     * @param password the password to validate
     * @throws AuthenticationException if password doesn't meet requirements
     */
    private void validatePassword(String password) throws AuthenticationException {
        if (password == null || password.length() < 8) {
            throw new AuthenticationException("Password must be at least 8 characters long");
        }

        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            throw new AuthenticationException("Password must contain at least one digit");
        }

        // Check for at least one letter
        if (!password.matches(".*[a-zA-Z].*")) {
            throw new AuthenticationException("Password must contain at least one letter");
        }

        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new AuthenticationException("Password must contain at least one special character");
        }
    }

    /**
     * Custom exception for authentication operations
     */
    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}