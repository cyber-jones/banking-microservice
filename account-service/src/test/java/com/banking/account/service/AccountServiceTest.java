package com.banking.account.service;

import com.banking.account.dto.AccountDto;
import com.banking.account.event.AccountEvent;
import com.banking.account.model.Account;
import com.banking.account.repository.AccountRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AccountServiceTest — Unit tests for AccountService.
 *
 * TEACHING POINT — Unit Testing with Mockito:
 *
 * @ExtendWith(MockitoExtension.class)
 *   Activates Mockito annotations. Mockito creates test doubles (mocks)
 *   that replace real dependencies so we can test in isolation.
 *
 * @Mock       — creates a fake (mock) object; all methods return null/empty by default
 * @InjectMocks — creates the real class under test and injects the @Mock objects
 *
 * Key Mockito methods:
 *   when(...).thenReturn(...)   — stub a method to return a value
 *   when(...).thenThrow(...)    — stub a method to throw an exception
 *   verify(mock).method(...)   — assert a method was called
 *   verify(mock, never())      — assert a method was NEVER called
 *   ArgumentCaptor             — capture the argument passed to a mock method
 *
 * TEACHING POINT — AssertJ (assertThat):
 * More readable than JUnit's Assertions.assertEquals. Chainable, fluent API:
 *   assertThat(result).isNotNull().isEqualTo(expected)
 *   assertThat(list).hasSize(3).contains(item)
 *   assertThatThrownBy(() -> ...).isInstanceOf(RuntimeException.class)
 *
 * TEACHING POINT — @Nested:
 * Groups related tests together for better organisation and readability.
 *
 * TEACHING POINT — @DisplayName:
 * Gives tests human-readable names in the test report.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaTemplate<String, AccountEvent> kafkaTemplate;

    @InjectMocks
    private AccountService accountService;

    private Account sampleAccount;
    private AccountDto.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        // TEACHING POINT: @BeforeEach runs before EVERY test method.
        // Use it to set up common test data so you don't repeat yourself.
        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC0000000001")
                .ownerName("John Doe")
                .email("john@example.com")
                .balance(new BigDecimal("1000.00"))
                .accountType(Account.AccountType.CHECKING)
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = new AccountDto.CreateRequest(
                "John Doe", "john@example.com",
                new BigDecimal("1000.00"), Account.AccountType.CHECKING
        );
    }

    // ──────────────────────────────────────────────────
    //  CREATE ACCOUNT TESTS
    // ──────────────────────────────────────────────────
    @Nested
    @DisplayName("createAccount()")
    class CreateAccountTests {

        @Test
        @DisplayName("Should create account successfully and return response DTO")
        void createAccount_Success() {
            // ARRANGE — set up stubs
            when(accountRepository.existsByEmail(anyString())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

            // ACT — call the method under test
            AccountDto.Response result = accountService.createAccount(createRequest);

            // ASSERT — verify the result
            assertThat(result).isNotNull();
            assertThat(result.getOwnerName()).isEqualTo("John Doe");
            assertThat(result.getEmail()).isEqualTo("john@example.com");
            assertThat(result.getBalance()).isEqualByComparingTo("1000.00");
            assertThat(result.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);

            // Verify repository was called exactly once
            verify(accountRepository, times(1)).save(any(Account.class));
        }

        @Test
        @DisplayName("Should publish Kafka event after account creation")
        void createAccount_PublishesKafkaEvent() {
            // ARRANGE
            when(accountRepository.existsByEmail(anyString())).thenReturn(false);
            when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

            // ACT
            accountService.createAccount(createRequest);

            // ASSERT — use ArgumentCaptor to inspect the Kafka message
            ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

            verify(kafkaTemplate, times(1)).send(
                    topicCaptor.capture(),
                    anyString(),
                    eventCaptor.capture()
            );

            assertThat(topicCaptor.getValue()).isEqualTo("account-events");
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_CREATED");
            assertThat(eventCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void createAccount_DuplicateEmail_ThrowsException() {
            // ARRANGE — email already in use
            when(accountRepository.existsByEmail(anyString())).thenReturn(true);

            // ASSERT — test that exception is thrown
            // TEACHING POINT: assertThatThrownBy is the clean Mockito/AssertJ way
            assertThatThrownBy(() -> accountService.createAccount(createRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            // Verify save was NEVER called (early return on duplicate)
            verify(accountRepository, never()).save(any(Account.class));
            // Verify Kafka was NEVER called either
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }
    }

    // ──────────────────────────────────────────────────
    //  GET ACCOUNT TESTS
    // ──────────────────────────────────────────────────
    @Nested
    @DisplayName("getAccount()")
    class GetAccountTests {

        @Test
        @DisplayName("Should return account when found by ID")
        void getAccount_Found() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

            AccountDto.Response result = accountService.getAccount(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAccountNumber()).isEqualTo("ACC0000000001");
        }

        @Test
        @DisplayName("Should throw RuntimeException when account not found")
        void getAccount_NotFound_ThrowsException() {
            // TEACHING POINT: Optional.empty() simulates a DB miss
            when(accountRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getAccount(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    // ──────────────────────────────────────────────────
    //  GET ALL ACCOUNTS TESTS
    // ──────────────────────────────────────────────────
    @Nested
    @DisplayName("getAllAccounts()")
    class GetAllAccountsTests {

        @Test
        @DisplayName("Should return mapped list of all accounts")
        void getAllAccounts_ReturnsList() {
            Account second = Account.builder()
                    .id(2L).accountNumber("ACC0000000002")
                    .ownerName("Jane Smith").email("jane@example.com")
                    .balance(new BigDecimal("5000.00"))
                    .accountType(Account.AccountType.SAVINGS)
                    .status(Account.AccountStatus.ACTIVE)
                    .createdAt(LocalDateTime.now()).build();

            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount, second));

            List<AccountDto.Response> result = accountService.getAllAccounts();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(AccountDto.Response::getEmail)
                    .containsExactly("john@example.com", "jane@example.com");
        }

        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void getAllAccounts_Empty() {
            when(accountRepository.findAll()).thenReturn(List.of());

            List<AccountDto.Response> result = accountService.getAllAccounts();

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────
    //  CLOSE ACCOUNT TESTS
    // ──────────────────────────────────────────────────
    @Nested
    @DisplayName("closeAccount()")
    class CloseAccountTests {

        @Test
        @DisplayName("Should set status to CLOSED and publish Kafka event")
        void closeAccount_Success() {
            when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

            accountService.closeAccount(1L);

            // Capture what was saved — verify status changed
            ArgumentCaptor<Account> savedCaptor = ArgumentCaptor.forClass(Account.class);
            verify(accountRepository).save(savedCaptor.capture());
            assertThat(savedCaptor.getValue().getStatus()).isEqualTo(Account.AccountStatus.CLOSED);

            // Verify ACCOUNT_CLOSED event was published
            ArgumentCaptor<AccountEvent> eventCaptor = ArgumentCaptor.forClass(AccountEvent.class);
            verify(kafkaTemplate).send(anyString(), anyString(), eventCaptor.capture());
            assertThat(eventCaptor.getValue().getEventType()).isEqualTo("ACCOUNT_CLOSED");
        }
    }
}
