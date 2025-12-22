package com.storefront.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private Key key;
    private final long jwtExpirationInMs = 3600000; // 1h

    @PostConstruct
    public void init() {
        // In prod, read from properties
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        logger.warn("JWT signing key initialized (NEW KEY GENERATED - all previous tokens are now invalid)");
    }

    public String generateToken(String username, String role, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        logger.debug("Parsing JWT to extract username...");
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.getSubject();
            logger.debug("Claims extracted - Subject: {}, Role: {}, UserId: {}",
                    username, claims.get("role"), claims.get("userId"));
            return username;
        } catch (Exception ex) {
            logger.error("Error parsing JWT claims: {}", ex.getMessage());
            throw ex;
        }
    }

    public boolean validateToken(String authToken) {
        logger.debug("Validating JWT token...");
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            logger.info("✓ JWT token validation SUCCESSFUL");
            return true;
        } catch (io.jsonwebtoken.security.SecurityException ex) {
            logger.error("✗ Invalid JWT signature: {}", ex.getMessage(), ex);
        } catch (io.jsonwebtoken.MalformedJwtException ex) {
            logger.error("✗ Malformed JWT token: {}", ex.getMessage(), ex);
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            logger.error("✗ JWT token EXPIRED at {}: {}", ex.getClaims().getExpiration(), ex.getMessage());
        } catch (io.jsonwebtoken.UnsupportedJwtException ex) {
            logger.error("✗ Unsupported JWT token: {}", ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            logger.error("✗ JWT claims string is empty: {}", ex.getMessage(), ex);
        } catch (Exception ex) {
            logger.error("✗ Unexpected error validating JWT: {}", ex.getMessage(), ex);
        }
        return false;
    }
}
