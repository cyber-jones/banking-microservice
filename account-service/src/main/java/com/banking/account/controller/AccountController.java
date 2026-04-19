package com.banking.account.controller;

import com.banking.account.dto.AccountDto;
import com.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccountController — the REST API layer.
 *
 * TEACHING POINT — Spring MVC REST Annotations:
 *
 * @RestController  = @Controller + @ResponseBody
 *   Automatically serialises return values to JSON (via Jackson)
 *
 * @RequestMapping  — base URL for all endpoints in this controller
 *
 * @GetMapping    → HTTP GET    (read/query data)
 * @PostMapping   → HTTP POST   (create new resource)
 * @PutMapping    → HTTP PUT    (replace entire resource)
 * @PatchMapping  → HTTP PATCH  (partial update)
 * @DeleteMapping → HTTP DELETE (remove resource)
 *
 * @PathVariable   — extracts {id} from the URL path
 * @RequestBody    — deserialises JSON body into a Java object
 * @Valid          — triggers Bean Validation on the annotated parameter
 *
 * ResponseEntity<T> — gives full control over HTTP status code + body
 *
 * TEACHING POINT — Spring Security @PreAuthorize:
 * Method-level security using Spring Expression Language (SpEL).
 * hasRole('ADMIN')   — only users with ADMIN role can call this method
 * hasAnyRole(...)    — multiple allowed roles
 * isAuthenticated()  — any logged-in user
 *
 * TEACHING POINT — Swagger/OpenAPI annotations:
 * @Tag, @Operation, @ApiResponse — these appear in the Swagger UI at /swagger-ui.html
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/v1/accounts
     * Create a new bank account.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new account", description = "Creates a new bank account with an initial deposit")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<AccountDto.Response> createAccount(
            @Valid @RequestBody AccountDto.CreateRequest request) {
        log.info("POST /api/v1/accounts - Creating account for: {}", request.getEmail());
        AccountDto.Response response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/accounts/{id}
     * Get account by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountDto.Response> getAccount(
            @Parameter(description = "Account ID") @PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    /**
     * GET /api/v1/accounts/number/{accountNumber}
     * Get account by account number.
     */
    @GetMapping("/number/{accountNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<AccountDto.Response> getAccountByNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    /**
     * GET /api/v1/accounts
     * Get all accounts — ADMIN only.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all accounts (Admin only)")
    public ResponseEntity<List<AccountDto.Response>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * PUT /api/v1/accounts/{id}
     * Update account details.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update account details")
    public ResponseEntity<AccountDto.Response> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.UpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    /**
     * DELETE /api/v1/accounts/{id}
     * Close an account — ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Close an account (Admin only)")
    @ApiResponse(responseCode = "204", description = "Account closed successfully")
    public ResponseEntity<Void> closeAccount(@PathVariable Long id) {
        accountService.closeAccount(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/accounts/{id}/balance
     * Update account balance.
     */
    @PatchMapping("/{id}/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update account balance")
    public ResponseEntity<AccountDto.Response> updateAccountBalance(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.BalanceUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccountBalance(id, request));
    }

    /**
     * PATCH /api/v1/accounts/{id}/freeze
     * Freeze an account.
     */
    @PatchMapping("/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Freeze account")
    public ResponseEntity<AccountDto.Response> freezeAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.freezeAccount(id));
    }
}
