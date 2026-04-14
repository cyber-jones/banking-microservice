package com.banking.account.repository;

import com.banking.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Account Repository — data access layer using Spring Data JPA.
 *
 * TEACHING POINT — Spring Data JPA Repository:
 *
 * JpaRepository<Account, Long> gives you FREE CRUD methods:
 *   save(), findById(), findAll(), deleteById(), count(), existsById(), etc.
 *
 * DERIVED QUERY METHODS — Spring Data parses the method name and generates SQL:
 *   findByEmail         → SELECT * FROM accounts WHERE email = ?
 *   findByAccountNumber → SELECT * FROM accounts WHERE account_number = ?
 *   findByStatus        → SELECT * FROM accounts WHERE status = ?
 *   existsByEmail       → SELECT COUNT(*) > 0 FROM accounts WHERE email = ?
 *
 * @Query JPQL — for complex queries, write JPQL (object-oriented SQL):
 *   References entity class names (Account) not table names (accounts)
 *   References field names (balance) not column names
 *
 * @Repository — optional with Spring Data JPA (auto-detected), but makes intent clear
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByStatus(Account.AccountStatus status);

    List<Account> findByOwnerNameContainingIgnoreCase(String name);

    boolean existsByEmail(String email);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * TEACHING POINT — @Query with JPQL.
     * Find all accounts with balance below a threshold (for overdraft monitoring).
     */
    @Query("SELECT a FROM Account a WHERE a.balance < :threshold AND a.status = 'ACTIVE'")
    List<Account> findLowBalanceAccounts(@Param("threshold") BigDecimal threshold);

    /**
     * TEACHING POINT — Native SQL query via nativeQuery = true.
     * Use sparingly — JPQL is preferred for portability.
     */
    @Query(value = "SELECT COUNT(*) FROM accounts WHERE account_type = :type", nativeQuery = true)
    long countByAccountType(@Param("type") String type);
}
