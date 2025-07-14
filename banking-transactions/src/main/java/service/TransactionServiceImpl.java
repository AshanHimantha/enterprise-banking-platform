package service;

import dto.TransactionDTO;
import dto.TransactionRequestDTO;
import entity.Account;
import entity.Transaction;
import entity.User;
import enums.TransactionStatus;
import enums.TransactionType;
import enums.UserStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class TransactionServiceImpl implements TransactionService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED) // Ensures this whole method is one atomic database transaction
    public void performTransfer(String username, TransactionRequestDTO request) {
        // 1. Find the user making the request
        User user = findUserByUsername(username);

        // 2. Find and apply a pessimistic lock to the accounts to prevent race conditions
        Account fromAccount = findAndLockAccount(request.getFromAccountNumber());
        Account toAccount = findAndLockAccount(request.getToAccountNumber());

        // 3. Perform Validations (Security, Business Rules)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Your account is not active. Please contact support.");
        }
        if (!fromAccount.getOwner().equals(user)) {
            throw new SecurityException("Authorization error: You do not own the source account.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be a positive value.");
        }
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds for this transfer.");
        }
        if(fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())){
            throw new IllegalArgumentException("Cannot transfer funds to the same account.");
        }

        // *** THE ARITHMETIC FIX ***
        // 4. Perform the debit and credit using BigDecimal's precise math methods
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(newFromBalance);

        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(newToBalance);

        // 5. Create and persist the transaction log
        Transaction transactionLog = new Transaction();
        transactionLog.setTransactionType(TransactionType.TRANSFER);
        transactionLog.setStatus(TransactionStatus.COMPLETED);
        transactionLog.setFromAccount(fromAccount);
        transactionLog.setToAccount(toAccount);
        transactionLog.setAmount(request.getAmount());
        transactionLog.setTransactionDate(LocalDateTime.now());
        transactionLog.setDescription("Transfer to " + toAccount.getOwner().getFirstName());
        transactionLog.setUserMemo(request.getUserMemo());
        transactionLog.setRunningBalance(newFromBalance); // Store the new balance of the sender's account

        em.persist(transactionLog);
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public List<TransactionDTO> getTransactionHistory(
            String username, String accountNumber, LocalDate startDate, LocalDate endDate,
            TransactionType transactionType, int pageNumber, int pageSize) {

        // Security check remains the same
        Account account = findAccountByNumber(accountNumber);
        if (account.getOwner() == null || !account.getOwner().getUsername().equals(username)) {
            throw new SecurityException("Authorization error: You do not own this account.");
        }

        // --- Dynamic Query Building ---
        StringBuilder jpql = new StringBuilder(
                "SELECT t FROM Transaction t WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)");

        // Use a Map to hold the query parameters
        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("accountId", account.getId());

        if (startDate != null) {
            jpql.append(" AND t.transactionDate >= :startDate");
            parameters.put("startDate", startDate.atStartOfDay());
        }
        if (endDate != null) {
            jpql.append(" AND t.transactionDate <= :endDate");
            parameters.put("endDate", endDate.plusDays(1).atStartOfDay()); // Include the whole end day
        }
        if (transactionType != null) {
            jpql.append(" AND t.transactionType = :transactionType");
            parameters.put("transactionType", transactionType);
        }

        jpql.append(" ORDER BY t.transactionDate DESC");

        // --- Create and Execute Query with Pagination ---
        TypedQuery<Transaction> query = em.createQuery(jpql.toString(), Transaction.class);

        // Set all the parameters we collected
        for (java.util.Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        // Apply pagination
        query.setFirstResult((pageNumber - 1) * pageSize); // Calculate the starting row
        query.setMaxResults(pageSize); // Set the number of rows to retrieve

        return query.getResultList().stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }
    // --- Helper Methods ---

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User '" + username + "' not found.");
        }
    }

    private Account findAccountByNumber(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Account with number '" + accountNumber + "' not found.");
        }
    }

    private Account findAndLockAccount(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Account with number '" + accountNumber + "' not found.");
        }
    }
}