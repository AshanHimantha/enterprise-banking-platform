package service.impl;

import annotation.Audit;
import annotation.Logging;
import dto.DepositHistoryDTO;
import dto.DepositRequestDTO;
import entity.Account;
import entity.Deposit;
import entity.Transaction;
import enums.TransactionStatus;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.*;
import service.DepositService;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Audit
@Logging
@Stateless
@RolesAllowed({"EMPLOYEE","ADMIN"})
@Interceptors(LoggingInterceptor.class)
public class DepositServiceImpl implements DepositService {
    @PersistenceContext
    private EntityManager em;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void processDeposit(String employeeUsername, DepositRequestDTO request) {
        // 1. Find the destination account and lock it
        Account toAccount = findAndLockAccount(request.getToAccountNumber());

        // 2. Perform Validations
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        // 3. Update the account balance
        BigDecimal newBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(newBalance);
        em.merge(toAccount);

        // 4. Create the core Transaction record
        Transaction txLog = new Transaction();
        txLog.setTransactionType(TransactionType.DEPOSIT);
        txLog.setStatus(TransactionStatus.COMPLETED);
        txLog.setFromAccount(null); // Deposits come from an external source (cash)
        txLog.setToAccount(toAccount);
        txLog.setAmount(request.getAmount());
        txLog.setDescription("Cash deposit processed by employee " + employeeUsername);
        txLog.setTransactionDate(LocalDateTime.now());
        txLog.setRunningBalance(newBalance);
        em.persist(txLog);

        // 5. Create the detailed Deposit audit record
        Deposit depositRecord = new Deposit();
        depositRecord.setTransaction(txLog); // Link to the main transaction record
        depositRecord.setToAccount(toAccount);
        depositRecord.setAmount(request.getAmount());
        depositRecord.setProcessedByEmployee(employeeUsername);
        depositRecord.setDepositTimestamp(LocalDateTime.now());
        depositRecord.setNotes(request.getNotes());
        em.persist(depositRecord);
    }

    private Account findAndLockAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty.");
        }

        try {
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber", Account.class);

            query.setParameter("accountNumber", accountNumber);

            // This is the most important line. It tells the JPA provider to
            // issue a "SELECT ... FOR UPDATE" statement to the database,
            // locking the row that is returned.
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

            return query.getSingleResult();

        } catch (NoResultException e) {
            // Provide a clear error message if the account does not exist.
            throw new IllegalArgumentException("Account with number '" + accountNumber + "' not found.");
        }
    }


    @Override
    public List<DepositHistoryDTO> getDepositHistory(
            String searchTerm, LocalDate startDate, LocalDate endDate, int pageNumber, int pageSize) {

        StringBuilder jpql = new StringBuilder("SELECT d FROM Deposit d WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();
        buildHistoryWhereClause(jpql, parameters, searchTerm, startDate, endDate);
        jpql.append(" ORDER BY d.depositTimestamp DESC");

        TypedQuery<Deposit> query = em.createQuery(jpql.toString(), Deposit.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList().stream()
                .map(DepositHistoryDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public long countDepositHistory(String searchTerm, LocalDate startDate, LocalDate endDate) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(d) FROM Deposit d WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();
        buildHistoryWhereClause(jpql, parameters, searchTerm, startDate, endDate);

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query.getSingleResult();
    }

    private void buildHistoryWhereClause(StringBuilder jpql, Map<String, Object> params, String term, LocalDate start, LocalDate end) {
        if (term != null && !term.trim().isEmpty()) {
            jpql.append(" AND (LOWER(d.toAccount.accountNumber) LIKE :term OR " +
                    "LOWER(d.toAccount.owner.username) LIKE :term OR " +
                    "LOWER(d.processedByEmployee) LIKE :term)");
            params.put("term", "%" + term.toLowerCase().trim() + "%");
        }
        if (start != null) {
            jpql.append(" AND d.depositTimestamp >= :start");
            params.put("start", start.atStartOfDay());
        }
        if (end != null) {
            jpql.append(" AND d.depositTimestamp < :end");
            params.put("end", end.plusDays(1).atStartOfDay());
        }
    }
}
