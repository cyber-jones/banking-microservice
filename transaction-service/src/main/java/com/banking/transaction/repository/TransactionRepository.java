package com.banking.transaction.repository;

import com.banking.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<Transaction> findByAccountNumberAndTypeOrderByCreatedAtDesc(
            String accountNumber, Transaction.TransactionType type);

    Optional<Transaction> findByTransactionRef(String ref);

    // TEACHING POINT — Aggregation with @Query (JPQL)
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.accountNumber = :acct AND t.type = 'DEPOSIT' AND t.status = 'COMPLETED'")
    BigDecimal sumDeposits(@Param("acct") String accountNumber);

    @Query("SELECT SUM(t.amount) FROM Transaction t " +
           "WHERE t.accountNumber = :acct AND t.type = 'WITHDRAWAL' AND t.status = 'COMPLETED'")
    BigDecimal sumWithdrawals(@Param("acct") String accountNumber);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.accountNumber = :acct")
    long countByAccount(@Param("acct") String accountNumber);

    List<Transaction> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
