package com.banking.account.service;

import com.banking.account.dto.AccountDto;
import com.banking.account.event.AccountEvent;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * AccountService — the business logic layer.
 *
 * TEACHING POINT — Service Layer Pattern (3-Tier Architecture):
 *   Controller → Service → Repository
 *   - Controller handles HTTP (request/response mapping)
 *   - Service handles business rules and orchestration
 *   - Repository handles database access
 *   Never put business logic in a Controller or Repository!
 *
 * @Service      — marks this as a Spring-managed service bean
 * @Transactional — wraps methods in a DB transaction (auto-commit/rollback)
 * @Slf4j        — injects a logger (log.info, log.error, etc.)
 * @RequiredArgsConstructor — generates a constructor for all final fields (= Dependency Injection)
 *
 * TEACHING POINT — Dependency Injection:
 * Spring injects AccountRepository and KafkaTemplate automatically.
 * We use constructor injection (via @RequiredArgsConstructor) — this is the
 * recommended approach over @Autowired field injection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;

    private static final String ACCOUNT_TOPIC = "account-events";

    /**
     * Create a new bank account.
     * Demonstrates: validation, entity mapping, persistence, Kafka event publishing.
     */
    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest request) {
        log.info("Creating account for owner: {}", request.getOwnerName());

        // Business rule: one email per account
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Account with email " + request.getEmail() + " already exists");
        }

        // Map DTO → Entity using Builder pattern
        Account account = Account.builder()
                .ownerName(request.getOwnerName())
                .email(request.getEmail())
                .balance(request.getInitialDeposit())
                .accountType(request.getAccountType())
                .accountNumber(generateAccountNumber())
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);

        // TEACHING POINT — Kafka event publishing:
        // After creating the account, publish an event so other services can react.
        // This is "event-driven architecture" — loose coupling between services.
        AccountEvent event = AccountEvent.builder()
                .eventType("ACCOUNT_CREATED")
                .accountId(saved.getId())
                .accountNumber(saved.getAccountNumber())
                .ownerName(saved.getOwnerName())
                .email(saved.getEmail())
                .build();

        kafkaTemplate.send(ACCOUNT_TOPIC, saved.getAccountNumber(), event);
        log.info("Published ACCOUNT_CREATED event for account: {}", saved.getAccountNumber());

        return mapToResponse(saved);
    }

    /**
     * Retrieve account by ID.
     * TEACHING POINT — Optional handling in Java/Spring:
     * orElseThrow provides a clean way to handle missing resources.
     */
    @Transactional(readOnly = true)
    public AccountDto.Response getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountDto.Response getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update account details.
     * TEACHING POINT — @Transactional ensures the update is atomic.
     * If anything throws after findById, the entire transaction rolls back.
     */
    @Transactional
    public AccountDto.Response updateAccount(Long id, AccountDto.UpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (request.getOwnerName() != null) {
            account.setOwnerName(request.getOwnerName());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail());
        }

        Account updated = accountRepository.save(account);
        return mapToResponse(updated);
    }

    /**
     * Close/deactivate an account (soft delete — we keep the record).
     */
    @Transactional
    public void closeAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);

        AccountEvent event = AccountEvent.builder()
                .eventType("ACCOUNT_CLOSED")
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .email(account.getEmail())
                .build();
        kafkaTemplate.send(ACCOUNT_TOPIC, account.getAccountNumber(), event);
        log.info("Account closed: {}", account.getAccountNumber());
    }

    /**
     * Update account Balance
     */
    @Transactional
    public AccountDto.Response updateAccountBalance(Long id, AccountDto.BalanceUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        if (request.getNewBalance()  != null)
            account.setBalance(request.getNewBalance());

        Account updated = accountRepository.save(account);
        return mapToResponse(updated);
    }


    /**
     * freeze/deactivate an account.
     */
    @Transactional
    public void freezeAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with id: " + id));

        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);

        AccountEvent event = AccountEvent.builder()
                .eventType("ACCOUNT_FROZEN")
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .email(account.getEmail())
                .build();
        kafkaTemplate.send(ACCOUNT_TOPIC, account.getAccountNumber(), event);
        log.info("Account frozen: {}", account.getAccountNumber());
    }

    /**
     * TEACHING POINT — Private helper to map Entity → Response DTO.
     * Always keep this mapping in the Service layer, not the Controller.
     */
    private AccountDto.Response mapToResponse(Account account) {
        return AccountDto.Response.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .email(account.getEmail())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private String generateAccountNumber() {
        return "ACC" + String.format("%010d", new Random().nextLong(9_000_000_000L) + 1_000_000_000L);
    }
}
