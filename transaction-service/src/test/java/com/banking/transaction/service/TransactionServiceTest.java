package com.banking.transaction.service;

import com.banking.transaction.client.AccountServiceClient;
import com.banking.transaction.dto.AccountClientDto;
import com.banking.transaction.dto.TransactionDto;
import com.banking.transaction.event.TransactionEvent;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TransactionServiceTest — unit tests for all transaction operations.
 *
 * TEACHING POINT — Testing the Service Layer in Isolation:
 * We mock AccountServiceClient so tests never make real HTTP calls.
 * This means tests run fast and don't need other services running.
 *
 * Pattern for each test:
 *  1. ARRANGE — set up mocks and input data
 *  2. ACT     — call the method under test
 *  3. ASSERT  — verify the result and any side effects
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @InjectMocks
    private TransactionService transactionService;

    private AccountClientDto.AccountResponse activeAccount;

    @BeforeEach
    void setUp() {
        activeAccount = new AccountClientDto.AccountResponse();
        activeAccount.setId(1L);
        activeAccount.setAccountNumber("ACC0000000001");
        activeAccount.setOwnerName("John Doe");
        activeAccount.setBalance(new BigDecimal("5000.00"));
        activeAccount.setStatus("ACTIVE");
    }

    // ─────────────────────────────────────────────────────────────
    // DEPOSIT TESTS
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("should process deposit and return updated balance")
        void deposit_ValidRequest_ReturnsResponse() {
            // ARRANGE
            TransactionDto.DepositRequest request = TransactionDto.DepositRequest.builder()
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("1000.00"))
                    .description("Salary payment")
                    .build();

            Transaction savedTx = Transaction.builder()
                    .id(1L)
                    .transactionRef("TXN-ABC123")
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("1000.00"))
                    .type(Transaction.TransactionType.DEPOSIT)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .balanceAfter(new BigDecimal("6000.00"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(accountServiceClient.getAccountByNumber("ACC0000000001")).thenReturn(activeAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTx);

            // ACT
            TransactionDto.Response result = transactionService.deposit(request);

            // ASSERT
            assertThat(result).isNotNull();
            assertThat(result.getTransactionRef()).isEqualTo("TXN-ABC123");
            assertThat(result.getAmount()).isEqualByComparingTo("1000.00");
            assertThat(result.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
            assertThat(result.getStatus()).isEqualTo(Transaction.TransactionStatus.COMPLETED);

            // Verify balance was updated on Account Service
            verify(accountServiceClient).updateBalance(eq(1L), any(AccountClientDto.BalanceUpdateRequest.class));

            // TEACHING POINT — ArgumentCaptor: inspect what was actually saved
            ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
            verify(transactionRepository).save(txCaptor.capture());
            Transaction capturedTx = txCaptor.getValue();
            assertThat(capturedTx.getBalanceAfter()).isEqualByComparingTo("6000.00"); // 5000 + 1000

            // Verify Kafka event was published
            verify(kafkaTemplate).send(eq("transaction-events"), eq("ACC0000000001"), any(TransactionEvent.class));
        }

        @Test
        @DisplayName("should throw exception when account is not active")
        void deposit_InactiveAccount_ThrowsException() {
            // ARRANGE
            activeAccount.setStatus("FROZEN");
            when(accountServiceClient.getAccountByNumber("ACC0000000001")).thenReturn(activeAccount);

            TransactionDto.DepositRequest request = TransactionDto.DepositRequest.builder()
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("100.00"))
                    .build();

            // ACT & ASSERT
            assertThatThrownBy(() -> transactionService.deposit(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not active");

            // Verify nothing was saved or published
            verify(transactionRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // WITHDRAWAL TESTS
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("should process withdrawal when sufficient funds exist")
        void withdraw_SufficientFunds_ReturnsResponse() {
            // ARRANGE
            TransactionDto.WithdrawalRequest request = TransactionDto.WithdrawalRequest.builder()
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("500.00"))
                    .description("ATM withdrawal")
                    .build();

            Transaction savedTx = Transaction.builder()
                    .id(2L)
                    .transactionRef("TXN-DEF456")
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("500.00"))
                    .type(Transaction.TransactionType.WITHDRAWAL)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .balanceAfter(new BigDecimal("4500.00"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(accountServiceClient.getAccountByNumber("ACC0000000001")).thenReturn(activeAccount);
            when(transactionRepository.save(any())).thenReturn(savedTx);

            // ACT
            TransactionDto.Response result = transactionService.withdraw(request);

            // ASSERT
            assertThat(result.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
            assertThat(result.getBalanceAfter()).isEqualByComparingTo("4500.00");
            verify(kafkaTemplate).send(anyString(), anyString(), any(TransactionEvent.class));
        }

        @Test
        @DisplayName("should throw exception when insufficient funds")
        void withdraw_InsufficientFunds_ThrowsException() {
            // ARRANGE — request more than available balance
            TransactionDto.WithdrawalRequest request = TransactionDto.WithdrawalRequest.builder()
                    .accountNumber("ACC0000000001")
                    .amount(new BigDecimal("99999.00"))  // more than 5000.00 balance
                    .build();

            when(accountServiceClient.getAccountByNumber("ACC0000000001")).thenReturn(activeAccount);

            // ACT & ASSERT
            assertThatThrownBy(() -> transactionService.withdraw(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient funds");

            verify(transactionRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // TRANSFER TESTS
    // ─────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Transfer Tests")
    class TransferTests {

        @Test
        @DisplayName("should complete transfer between two active accounts")
        void transfer_ValidAccounts_CompletesTransfer() {
            // ARRANGE
            AccountClientDto.AccountResponse toAccount = new AccountClientDto.AccountResponse();
            toAccount.setId(2L);
            toAccount.setAccountNumber("ACC0000000002");
            toAccount.setBalance(new BigDecimal("2000.00"));
            toAccount.setStatus("ACTIVE");

            when(accountServiceClient.getAccountByNumber("ACC0000000001")).thenReturn(activeAccount);
            when(accountServiceClient.getAccountByNumber("ACC0000000002")).thenReturn(toAccount);

            Transaction savedTx = Transaction.builder()
                    .id(3L)
                    .transactionRef("TXN-GHI789")
                    .accountNumber("ACC0000000001")
                    .targetAccountNumber("ACC0000000002")
                    .amount(new BigDecimal("300.00"))
                    .type(Transaction.TransactionType.TRANSFER)
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .balanceAfter(new BigDecimal("4700.00"))
                    .createdAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.save(any())).thenReturn(savedTx);

            TransactionDto.TransferRequest request = TransactionDto.TransferRequest.builder()
                    .fromAccountNumber("ACC0000000001")
                    .toAccountNumber("ACC0000000002")
                    .amount(new BigDecimal("300.00"))
                    .description("Rent")
                    .build();

            // ACT
            TransactionDto.Response result = transactionService.transfer(request);

            // ASSERT
            assertThat(result.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);

            // Both accounts should have balance updated
            verify(accountServiceClient, times(2)).updateBalance(anyLong(), any());
            verify(kafkaTemplate).send(anyString(), anyString(), any(TransactionEvent.class));
        }
    }
}
