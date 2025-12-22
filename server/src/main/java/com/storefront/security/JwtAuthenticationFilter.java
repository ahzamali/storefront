package com.storefront.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        logger.debug("==================== JWT FILTER START ====================");
        logger.debug("Request: {} {}", request.getMethod(), request.getRequestURI());

        // Log Authorization header specifically
        String authHeader = request.getHeader("Authorization");
        logger.debug("Authorization header: {}",
                authHeader != null ? (authHeader.length() > 30 ? authHeader.substring(0, 30) + "..." : authHeader)
                        : "null");

        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            // Log token preview
            String tokenPreview = jwt.length() > 20 ? jwt.substring(0, 10) + "..." + jwt.substring(jwt.length() - 10)
                    : jwt;
            logger.debug("JWT token extracted: {}", tokenPreview);

            logger.debug("Calling tokenProvider.validateToken()...");
            boolean isValid = tokenProvider.validateToken(jwt);
            logger.debug("Token validation result: {}", isValid);

            if (isValid) {
                logger.debug("Extracting username from JWT...");
                String username = tokenProvider.getUsernameFromJWT(jwt);
                logger.debug("Username from token: {}", username);

                logger.debug("Loading user details from database...");
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.debug("User loaded with authorities: {}", userDetails.getAuthorities());

                logger.debug("Creating authentication object...");
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                logger.debug("Setting authentication in SecurityContext...");
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("✓ AUTHENTICATION SUCCESS - User: {}, Authorities: {}",
                        username, userDetails.getAuthorities());
            } else {
                logger.debug("✗ JWT validation FAILED - authentication not set");
            }
        } else {
            logger.debug("No JWT token found in Authorization header");
        }

        Object auth = SecurityContextHolder.getContext().getAuthentication();
        logger.debug("Authentication in SecurityContext before filter chain: {}",
                auth != null ? auth.toString() : "null");
        logger.debug("==================== JWT FILTER END ====================");

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
