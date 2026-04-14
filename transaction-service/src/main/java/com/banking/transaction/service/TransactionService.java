package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.AccountClientDto;
import com.banking.transaction.dto.TransactionDto;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TransactionService — handles all financial transaction logic.
 *
 * TEACHING POINT — @Transactional and Distributed Systems:
 * @Transactional only covers THIS service's database operations.
 * When we call AccountService via Feign, that's a SEPARATE transaction.
 * If our DB commit succeeds but the Feign call fails, we have inconsistency.
 *
 * Real-world solutions: SAGA pattern, Outbox pattern, or 2-Phase Commit.
 * For teaching, we use compensating transactions (mark as FAILED on error).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TRANSACTION_TOPIC = "transaction-events";

    @Transactional
    public TransactionDto.Response deposit(TransactionDto.DepositRequest request) {
        log.info("Processing deposit of {} to account {}", request.getAmount(), request.getAccountNumber());

        // 1. Verify account exists via FeignClient call to Account Service
        AccountClientDto.AccountResponse account =
                accountServiceClient.getAccountByNumber(request.getAccountNumber());

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalStateException("Account is not active: " + request.getAccountNumber());
        }

        // 2. Calculate new balance
        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());

        // 3. Create transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(generateRef())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .balanceAfter(newBalance)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // 4. Update balance in Account Service
        accountServiceClient.updateBalance(account.getId(),
                new AccountClientDto.BalanceUpdateRequest(newBalance));

        // 5. Publish event
        publishEvent("DEPOSIT_COMPLETED", saved);

        return mapToResponse(saved);
    }

    @Transactional
    public TransactionDto.Response withdraw(TransactionDto.WithdrawalRequest request) {
        AccountClientDto.AccountResponse account =
                accountServiceClient.getAccountByNumber(request.getAccountNumber());

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalStateException("Account is not active");
        }

        // Business rule: sufficient funds
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds. Balance: " +
                    account.getBalance() + ", Requested: " + request.getAmount());
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());

        Transaction transaction = Transaction.builder()
                .transactionRef(generateRef())
                .accountNumber(request.getAccountNumber())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .balanceAfter(newBalance)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        accountServiceClient.updateBalance(account.getId(),
                new AccountClientDto.BalanceUpdateRequest(newBalance));
        publishEvent("WITHDRAWAL_COMPLETED", saved);
        return mapToResponse(saved);
    }

    @Transactional
    public TransactionDto.Response transfer(TransactionDto.TransferRequest request) {
        // Get both accounts
        AccountClientDto.AccountResponse fromAccount =
                accountServiceClient.getAccountByNumber(request.getFromAccountNumber());
        AccountClientDto.AccountResponse toAccount =
                accountServiceClient.getAccountByNumber(request.getToAccountNumber());

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds for transfer");
        }

        BigDecimal fromNewBalance = fromAccount.getBalance().subtract(request.getAmount());
        BigDecimal toNewBalance = toAccount.getBalance().add(request.getAmount());

        Transaction transaction = Transaction.builder()
                .transactionRef(generateRef())
                .accountNumber(request.getFromAccountNumber())
                .targetAccountNumber(request.getToAccountNumber())
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .balanceAfter(fromNewBalance)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        accountServiceClient.updateBalance(fromAccount.getId(),
                new AccountClientDto.BalanceUpdateRequest(fromNewBalance));
        accountServiceClient.updateBalance(toAccount.getId(),
                new AccountClientDto.BalanceUpdateRequest(toNewBalance));
        publishEvent("TRANSFER_COMPLETED", saved);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransactionDto.Response> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionDto.AccountSummary getAccountSummary(String accountNumber) {
        BigDecimal deposits = transactionRepository.sumDeposits(accountNumber);
        BigDecimal withdrawals = transactionRepository.sumWithdrawals(accountNumber);
        long count = transactionRepository.countByAccount(accountNumber);
        AccountClientDto.AccountResponse account =
                accountServiceClient.getAccountByNumber(accountNumber);

        return TransactionDto.AccountSummary.builder()
                .accountNumber(accountNumber)
                .totalDeposits(deposits != null ? deposits : BigDecimal.ZERO)
                .totalWithdrawals(withdrawals != null ? withdrawals : BigDecimal.ZERO)
                .transactionCount(count)
                .currentBalance(account.getBalance())
                .build();
    }

    private void publishEvent(String eventType, Transaction t) {
        TransactionEvent event = TransactionEvent.builder()
                .eventType(eventType)
                .transactionRef(t.getTransactionRef())
                .accountNumber(t.getAccountNumber())
                .amount(t.getAmount())
                .type(t.getType().name())
                .build();
        kafkaTemplate.send(TRANSACTION_TOPIC, t.getAccountNumber(), event);
    }

    private TransactionDto.Response mapToResponse(Transaction t) {
        return TransactionDto.Response.builder()
                .id(t.getId())
                .transactionRef(t.getTransactionRef())
                .accountNumber(t.getAccountNumber())
                .targetAccountNumber(t.getTargetAccountNumber())
                .amount(t.getAmount())
                .type(t.getType())
                .status(t.getStatus())
                .description(t.getDescription())
                .balanceAfter(t.getBalanceAfter())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private String generateRef() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
    }
}
