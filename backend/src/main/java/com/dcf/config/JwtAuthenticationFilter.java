package com.dcf.config;

import com.dcf.service.AuthenticationService;
import com.dcf.service.AuthenticationService.AuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private AuthenticationService authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        // Skip JWT validation for auth endpoints and public endpoints
        if (shouldSkipAuthentication(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = null;
        String jwt = null;

        // Extract JWT token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                userId = authenticationService.validateTokenAndGetUserId(jwt);
                logger.debug("JWT token validated for user: {}", userId);
            } catch (AuthenticationException e) {
                logger.warn("JWT token validation failed: {}", e.getMessage());
            }
        } else {
            logger.debug("No Authorization header found or invalid format for request: {}", requestURI);
        }

        // Set authentication in security context if token is valid
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            logger.debug("Authentication set in security context for user: {}", userId);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Determine if authentication should be skipped for this request
     * @param requestURI the request URI
     * @return true if authentication should be skipped
     */
    private boolean shouldSkipAuthentication(String requestURI) {
        // Skip authentication for auth endpoints
        if (requestURI.startsWith("/auth/")) {
            return true;
        }
        
        // Skip authentication for public endpoints
        if (requestURI.startsWith("/public/")) {
            return true;
        }
        
        // Skip authentication for health check endpoints
        if (requestURI.startsWith("/actuator/")) {
            return true;
        }
        
        // Skip authentication for error endpoints
        if (requestURI.startsWith("/error")) {
            return true;
        }

        return false;
    }
}