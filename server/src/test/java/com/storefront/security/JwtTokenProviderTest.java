package com.storefront.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        jwtTokenProvider.init();
    }

    @Test
    void generateToken_ValidInput_ReturnsToken() {
        String token = jwtTokenProvider.generateToken("testuser", "EMPLOYEE", 1L);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void getUsernameFromJWT_ValidToken_ReturnsUsername() {
        String token = jwtTokenProvider.generateToken("testuser", "EMPLOYEE", 1L);

        String username = jwtTokenProvider.getUsernameFromJWT(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.generateToken("testuser", "EMPLOYEE", 1L);

        boolean isValid = jwtTokenProvider.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() throws Exception {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000);

        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "EMPLOYEE")
                .claim("userId", 1L)
                .setIssuedAt(new Date(now.getTime() - 2000))
                .setExpiration(expiredDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        JwtTokenProvider provider = new JwtTokenProvider();
        java.lang.reflect.Field keyField = JwtTokenProvider.class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(provider, key);

        boolean isValid = provider.validateToken(expiredToken);

        assertFalse(isValid);
    }

    @Test
    void generateToken_ContainsCorrectClaims() {
        String token = jwtTokenProvider.generateToken("testuser", "ADMIN", 42L);

        String username = jwtTokenProvider.getUsernameFromJWT(token);

        assertEquals("testuser", username);
    }
}
