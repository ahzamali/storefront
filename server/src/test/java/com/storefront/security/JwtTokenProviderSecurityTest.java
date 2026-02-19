package com.storefront.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderSecurityTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.init();
    }

    @Test
    void validateToken_NullToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken(null);
        assertFalse(isValid);
    }

    @Test
    void validateToken_EmptyToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("");
        assertFalse(isValid);
    }

    @Test
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("not.a.valid.token");
        assertFalse(isValid);
    }

    @Test
    void validateToken_TamperedToken_ReturnsFalse() {
        String validToken = jwtTokenProvider.generateToken("user", "EMPLOYEE", 1L);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);
        assertFalse(isValid);
    }

    @Test
    void getUsernameFromJWT_InvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtTokenProvider.getUsernameFromJWT("invalid.token"));
    }

    @Test
    void generateToken_NullUsername_GeneratesToken() {
        String token = jwtTokenProvider.generateToken(null, "EMPLOYEE", 1L);
        assertNotNull(token);
    }

    @Test
    void generateToken_EmptyRole_GeneratesToken() {
        String token = jwtTokenProvider.generateToken("user", "", 1L);
        assertNotNull(token);
    }

    @Test
    void generateToken_NullUserId_GeneratesToken() {
        String token = jwtTokenProvider.generateToken("user", "EMPLOYEE", null);
        assertNotNull(token);
    }

    @Test
    void validateToken_TokenWithSpecialCharacters_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("token<script>alert('xss')</script>");
        assertFalse(isValid);
    }

    @Test
    void validateToken_SQLInjectionAttempt_ReturnsFalse() {
        boolean isValid = jwtTokenProvider.validateToken("' OR '1'='1");
        assertFalse(isValid);
    }
}
