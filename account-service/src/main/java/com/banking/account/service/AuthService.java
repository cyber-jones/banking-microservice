package com.banking.account.service;

import com.banking.account.dto.AccountDto;
import com.banking.account.model.User;
import com.banking.account.repository.UserRepository;
import com.banking.account.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * AuthService — handles user registration and JWT login.
 *
 * TEACHING POINT — Authentication Flow:
 *  1. Register: hash password with Bcrypt, save user to DB
 *  2. Login: AuthenticationManager verifies credentials,
 *     JwtUtil generates a signed token, return token to client
 *  3. Client includes "Authorization: Bearer <token>" in subsequent requests
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public void register(AccountDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // Bcrypt hash
                .email(request.getEmail())
                .roles(Set.of("ROLE_ADMIN"))
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", request.getUsername());
    }

    public AccountDto.LoginResponse login(AccountDto.LoginRequest request) {
        // AuthenticationManager checks username + password against DB
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return AccountDto.LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(request.getUsername())
                .expiresIn(jwtUtil.getExpiration())
                .build();
    }
}
