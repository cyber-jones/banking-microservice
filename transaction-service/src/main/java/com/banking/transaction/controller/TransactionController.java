package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionDto;
import com.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Deposit, Withdraw, Transfer and Query transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deposit funds into an account")
    public ResponseEntity<TransactionDto.Response> deposit(
            @Valid @RequestBody TransactionDto.DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionDto.Response> withdraw(
            @Valid @RequestBody TransactionDto.WithdrawalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(request));
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Transfer funds between two accounts")
    public ResponseEntity<TransactionDto.Response> transfer(
            @Valid @RequestBody TransactionDto.TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(request));
    }

    @GetMapping("/account/{accountNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all transactions for an account")
    public ResponseEntity<List<TransactionDto.Response>> getTransactions(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountNumber));
    }

    @GetMapping("/account/{accountNumber}/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction summary for an account")
    public ResponseEntity<TransactionDto.AccountSummary> getSummary(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getAccountSummary(accountNumber));
    }
}
