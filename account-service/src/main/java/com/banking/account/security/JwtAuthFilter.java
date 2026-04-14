package com.banking.account.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter — intercepts every HTTP request and validates the JWT.
 *
 * TEACHING POINT — Spring Security Filter Chain:
 *
 * Spring Security works as a chain of filters that process each HTTP request.
 * Our custom filter extends OncePerRequestFilter to run exactly once per request.
 *
 * Flow per request:
 *   1. Extract "Authorization: Bearer <token>" header
 *   2. Parse and validate the JWT
 *   3. Load UserDetails from database
 *   4. If valid, create an Authentication object and set it in SecurityContext
 *   5. Pass request down the filter chain
 *
 * Once authentication is in SecurityContextHolder, Spring Security
 * knows who the current user is for the entire request thread.
 *
 * If validation fails, the filter just lets the request continue unauthenticated
 * — the protected endpoint will then reject it with 401 Unauthorized.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token present — skip to next filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (everything after "Bearer ")
        final String jwt = authHeader.substring(7);

        try {
            final String username = jwtUtil.extractUsername(jwt);

            // Only process if we have a username and no existing authentication
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    // Create authentication token with authorities (roles)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Store in SecurityContext — this authenticates the request
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user: {}", username);
                }
            }
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
