package com.dcf.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class EmailUtil {
    
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    
    private static final Pattern pattern = Pattern.compile(EMAIL_PATTERN);
    
    /**
     * Validate email format
     * @param email the email to validate
     * @return true if email is valid, false otherwise
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return pattern.matcher(email).matches();
    }
    
    /**
     * Normalize email (convert to lowercase and trim)
     * @param email the email to normalize
     * @return normalized email
     */
    public String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
    
    /**
     * Get email validation error message
     * @param email the email to validate
     * @return error message or null if valid
     */
    public String getEmailValidationError(String email) {
        if (email == null || email.isEmpty()) {
            return "Email is required";
        }
        
        if (!isValidEmail(email)) {
            return "Please enter a valid email address";
        }
        
        return null; // Email is valid
    }
}