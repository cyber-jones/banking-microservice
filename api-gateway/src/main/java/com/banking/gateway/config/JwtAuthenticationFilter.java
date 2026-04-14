package com.banking.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;

/**
 * JwtAuthenticationFilter — a Gateway Filter that validates JWT on every request.
 *
 * TEACHING POINT — Gateway Filter vs. Servlet Filter:
 * Gateway uses WebFlux (reactive), so filters return Mono<Void> instead of void.
 * Reactive programming uses a non-blocking pipeline:
 *   chain.filter(exchange) — passes the request to the next filter
 *   exchange.getResponse().setComplete() — terminates the request with a response
 *
 * TEACHING POINT — AbstractGatewayFilterFactory:
 * Spring Cloud Gateway uses a factory pattern for filters.
 * This allows filters to be configured in application.yml per-route.
 *
 * How it works:
 *   1. Extract "Authorization: Bearer <token>" header
 *   2. Validate the JWT signature and expiry
 *   3. If valid: forward the request (and add user info as downstream header)
 *   4. If invalid: return 401 Unauthorized immediately
 *
 * TEACHING POINT — Forwarding user info downstream:
 * After validating the token, we add "X-Auth-User" header with the username.
 * Downstream services can read this header to know who made the request
 * WITHOUT re-validating the JWT themselves.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${app.jwt.secret}")
    private String secret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Check if Authorization header is present
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = validateToken(token);
                String username = claims.getSubject();
                log.debug("Gateway validated JWT for user: {}", username);

                // Forward the authenticated username as a header to downstream services
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-User", username)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.warn("JWT validation failed at gateway: {}", e.getMessage());
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Claims validateToken(String token) {
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

    /**
     * Returns an error response reactively.
     * TEACHING POINT: In reactive programming, we RETURN a Mono (never block).
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        log.warn("Gateway rejected request: {}", message);
        return response.setComplete();
    }

    /**
     * Config class — allows configuring the filter in application.yml.
     * Currently empty but can hold properties like excluded paths.
     */
    public static class Config {
        // Future: add excludedPaths, requireRoles, etc.
    }
}
