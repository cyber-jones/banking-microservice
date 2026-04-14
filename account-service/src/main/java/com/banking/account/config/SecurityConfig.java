package com.banking.account.config;

import com.banking.account.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig — the heart of Spring Security configuration.
 *
 * TEACHING POINT — Modern Spring Security (Spring Boot 3.x):
 * We use the new component-based approach with @Bean methods instead
 * of the old WebSecurityConfigurerAdapter (which is removed in Spring 6).
 *
 * Key concepts:
 *
 * SecurityFilterChain  — defines which endpoints are public vs protected
 * SessionCreationPolicy.STATELESS  — no HTTP sessions (JWT is stateless)
 * CSRF disabled  — safe for stateless REST APIs (no browser session cookies)
 *
 * DaoAuthenticationProvider  — authenticates users from the database
 *   - uses UserDetailsService to load user by username
 *   - uses PasswordEncoder to verify hashed password
 *
 * BCryptPasswordEncoder  — strong one-way password hashing
 *   NEVER store plain-text passwords. BCrypt adds salt automatically.
 *
 * @EnableMethodSecurity  — enables @PreAuthorize on controller methods
 * @EnableWebSecurity  — enables Spring Security's web security support
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * The main security filter chain.
     * This defines which requests require authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF — not needed for stateless JWT APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Define URL-level authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no token needed)
                        .requestMatchers(
                                "/api/v1/auth/**",           // login & register
                                "/h2-console/**",            // H2 browser console
                                "/swagger-ui/**",            // Swagger UI
                                "/swagger-ui.html",
                                "/v3/api-docs/**",           // OpenAPI JSON
                                "/actuator/health"           // health check
                        ).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // No HTTP sessions — JWT is our state
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Register our authentication provider (DB-backed)
                .authenticationProvider(authenticationProvider())

                // Add our JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Allow H2 console to display in iframe (X-Frame-Options)
                .headers(headers -> headers.frameOptions(fo -> fo.disable()))

                .build();
    }

    /**
     * AuthenticationProvider — wires together UserDetailsService + PasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder.
     * TEACHING POINT: BCrypt uses adaptive hashing — the "work factor" increases
     * over time to keep pace with faster hardware. Default work factor = 10.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager — needed by AuthService to authenticate login requests.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
