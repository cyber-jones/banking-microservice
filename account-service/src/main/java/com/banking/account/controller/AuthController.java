package com.banking.account.controller;

import com.banking.account.dto.AccountDto;
import com.banking.account.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — handles user registration and JWT login.
 *
 * TEACHING POINT — Public endpoints:
 * These endpoints are excluded from JWT authentication in SecurityConfig
 * because users need them to GET a token in the first place.
 *
 * Flow:
 *   1. POST /auth/register  → create a user account
 *   2. POST /auth/login     → get a JWT token
 *   3. Use Bearer token in Authorization header for all protected endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<String> register(@Valid @RequestBody AccountDto.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token",
               description = "Returns a Bearer JWT token. Use it in Authorization header for all other endpoints.")
    public ResponseEntity<AccountDto.LoginResponse> login(@Valid @RequestBody AccountDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
