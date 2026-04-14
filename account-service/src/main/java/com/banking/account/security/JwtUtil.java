package com.banking.account.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtil — creates and validates JSON Web Tokens.
 *
 * TEACHING POINT — How JWT Works:
 *
 *  A JWT has 3 parts separated by dots:
 *    header.payload.signature
 *
 *  1. Header  — algorithm used (e.g., HS256)
 *  2. Payload — "claims": username, roles, expiry, issued-at
 *  3. Signature — HMAC hash of header+payload using the secret key
 *
 *  The server never stores sessions — it just re-validates the signature
 *  on every request. This makes it stateless and scalable.
 *
 * TEACHING POINT — @Value:
 *  Injects values from application.yml/properties into fields.
 *  ${app.jwt.secret}   reads the "app.jwt.secret" property.
 *  ${app.jwt.expiration} reads the expiration duration in milliseconds.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    /**
     * Generate a JWT token for an authenticated user.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return buildToken(claims, userDetails.getUsername());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject)                              // "sub" = username
                .setIssuedAt(new Date())                          // "iat" = issued at
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // "exp"
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // Sign with secret
                .compact();
    }

    /**
     * Extract username from a token (reads the "sub" claim).
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validate a token: check username matches and token is not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpiration() {
        return expiration;
    }
}
