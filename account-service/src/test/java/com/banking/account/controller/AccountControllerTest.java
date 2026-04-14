package com.banking.account.controller;

import com.banking.account.dto.AccountDto;
import com.banking.account.model.Account;
import com.banking.account.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AccountControllerTest — Web layer (slice) tests using MockMvc.
 *
 * TEACHING POINT — @WebMvcTest (Slice Test):
 * Only loads the web layer (controllers, filters, security) — NOT the full context.
 * Services and repositories are NOT created; we mock them with @MockBean.
 * This makes tests fast and focused on HTTP behaviour.
 *
 * vs @SpringBootTest (full integration test):
 *   @WebMvcTest   — fast, web layer only, mock services
 *   @SpringBootTest — slow, full app, real beans, real DB
 *
 * MockMvc — simulates HTTP requests without starting a real server:
 *   perform(get("/api/v1/accounts/1"))   — simulate GET request
 *   .andExpect(status().isOk())          — assert HTTP 200
 *   .andExpect(jsonPath("$.id").value(1)) — assert JSON response field
 *
 * @WithMockUser — bypasses JWT filter, simulates an authenticated user with given roles.
 * TEACHING POINT: Without this, Spring Security rejects all requests with 401.
 *
 * @MockBean — creates a Mockito mock AND registers it as a Spring bean
 * (replaces the real bean in the Spring context for the duration of the test)
 */
@WebMvcTest(AccountController.class)
@DisplayName("AccountController Web Layer Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private AccountDto.Response sampleResponse() {
        return AccountDto.Response.builder()
                .id(1L)
                .accountNumber("ACC0000000001")
                .ownerName("John Doe")
                .email("john@example.com")
                .balance(new BigDecimal("1000.00"))
                .accountType(Account.AccountType.CHECKING)
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/accounts → 201 Created")
    @WithMockUser(username = "john", roles = {"USER"})
    void createAccount_Returns201() throws Exception {
        AccountDto.CreateRequest req = new AccountDto.CreateRequest(
                "John Doe", "john@example.com",
                new BigDecimal("1000.00"), Account.AccountType.CHECKING
        );

        when(accountService.createAccount(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").value("ACC0000000001"))
                .andExpect(jsonPath("$.ownerName").value("John Doe"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("POST /api/v1/accounts with invalid data → 400 Bad Request")
    @WithMockUser(roles = {"USER"})
    void createAccount_InvalidInput_Returns400() throws Exception {
        // Missing required fields
        AccountDto.CreateRequest req = new AccountDto.CreateRequest(
                "",       // blank name — violates @NotBlank
                "bad-email",  // invalid email — violates @Email
                new BigDecimal("-100"), // negative — violates @DecimalMin
                null      // null type — violates @NotNull
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/1 → 200 OK")
    @WithMockUser(roles = {"USER"})
    void getAccount_Returns200() throws Exception {
        when(accountService.getAccount(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts without auth → 401 Unauthorized")
    void getAccount_NoAuth_Returns401() throws Exception {
        // No @WithMockUser — request is unauthenticated
        mockMvc.perform(get("/api/v1/accounts/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/accounts/1 as USER → 403 Forbidden")
    @WithMockUser(roles = {"USER"})  // USER cannot delete — only ADMIN can
    void deleteAccount_AsUser_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/1").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/accounts/1 as ADMIN → 204 No Content")
    @WithMockUser(roles = {"ADMIN"})
    void deleteAccount_AsAdmin_Returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/accounts/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
