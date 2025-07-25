package service.impl;

import annotation.Audit;
import annotation.Logging;
import dto.BillPaymentRequestDTO;
import dto.TransactionDTO;
import dto.TransactionDetailDTO;
import dto.TransactionRequestDTO;
import entity.Account;
import entity.Biller;
import entity.Transaction;
import entity.User;
import enums.*;
import exception.AccountStatusException;
import exception.InsufficientFundsException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.TransactionService;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Audit
@Logging
@Stateless
@Interceptors(LoggingInterceptor.class)
public class TransactionServiceImpl implements TransactionService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED) // Ensures this whole method is one atomic database transaction
    public void performTransfer(String username, TransactionRequestDTO request) {

        User user = findUserByUsername(username);

        Account fromAccount = findAndLockAccount(request.getFromAccountNumber());
        Account toAccount = findAndLockAccount(request.getToAccountNumber());

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Your account is not active. Please contact support.");
        }
        if (!fromAccount.getOwner().equals(user)) {
            throw new SecurityException("Authorization error: You do not own the source account.");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be a positive value.");
        }
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds for this transfer.");
        }
        if(fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())){
            throw new IllegalArgumentException("Cannot transfer funds to the same account.");
        }

        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(newFromBalance);

        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(newToBalance);


        Transaction transactionLog = new Transaction();
        transactionLog.setTransactionType(TransactionType.TRANSFER);
        transactionLog.setStatus(TransactionStatus.COMPLETED);
        transactionLog.setFromAccount(fromAccount);
        transactionLog.setToAccount(toAccount);
        transactionLog.setAmount(request.getAmount());
        transactionLog.setTransactionDate(LocalDateTime.now());
        transactionLog.setDescription("Transfer to " + toAccount.getOwner().getFirstName());
        transactionLog.setUserMemo(request.getUserMemo());
        transactionLog.setRunningBalance(newFromBalance);

        em.persist(transactionLog);
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public Optional<TransactionDetailDTO> getTransactionDetails(String username, Long transactionId) {

        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t " +
                        "LEFT JOIN FETCH t.fromAccount fa LEFT JOIN FETCH fa.owner " +
                        "LEFT JOIN FETCH t.toAccount ta LEFT JOIN FETCH ta.owner " +
                        "WHERE t.id = :transactionId", Transaction.class);
        query.setParameter("transactionId", transactionId);

        Transaction transaction;
        try {
            transaction = query.getSingleResult();
        } catch (NoResultException e) {
            return Optional.empty(); // Not found
        }

        if (!isUserParticipant(username, transaction)) {
            throw new SecurityException("You are not authorized to view this transaction.");
        }


        TransactionDetailDTO dto = new TransactionDetailDTO();
        dto.setId(transaction.getId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setStatus(transaction.getStatus());
        dto.setAmount(transaction.getAmount());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setDescription(transaction.getDescription());
        dto.setUserMemo(transaction.getUserMemo());


        if (transaction.getFromAccount() != null && transaction.getFromAccount().getOwner() != null) {
            User fromOwner = transaction.getFromAccount().getOwner();
            dto.setFromAccountNumber(transaction.getFromAccount().getAccountNumber());
            dto.setFromOwnerName(fromOwner.getFirstName() + " " + fromOwner.getLastName());
            dto.setFromOwnerAvatarUrl(buildAvatarApiUrl(fromOwner.getProfilePictureUrl()));
        }


        if (transaction.getToAccount() != null) {
            if (transaction.getTransactionType() == TransactionType.BILL_PAYMENT) {

                Optional<Biller> billerOptional = findBillerByInternalAccount(transaction.getToAccount());
                if (billerOptional.isPresent()) {
                    Biller biller = billerOptional.get();
                    dto.setToOwnerName(biller.getBillerName());
                    dto.setToAccountNumber("BILLER");
                    dto.setToOwnerAvatarUrl(buildAvatarApiUrl(biller.getLogoUrl()));
                }
            } else if (transaction.getToAccount().getOwner() != null) {

                User toOwner = transaction.getToAccount().getOwner();
                dto.setToAccountNumber(transaction.getToAccount().getAccountNumber());
                dto.setToOwnerName(toOwner.getFirstName() + " " + toOwner.getLastName());
                dto.setToOwnerAvatarUrl(buildAvatarApiUrl(toOwner.getProfilePictureUrl()));
            }
        }

        return Optional.of(dto);
    }

    private String buildAvatarApiUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null; // Return null if no image is set
        }

        if(filename.contains("avatar")){
            return "/api/user/profile/avatar/image/" + filename;
        }else {
            return "/api/biller/logo/image/" + filename;
        }

    }

    // Helper method for the security check
    private boolean isUserParticipant(String username, Transaction transaction) {
        boolean isSender = transaction.getFromAccount() != null &&
                transaction.getFromAccount().getOwner() != null &&
                transaction.getFromAccount().getOwner().getUsername().equals(username);

        boolean isReceiver = transaction.getToAccount() != null &&
                transaction.getToAccount().getOwner() != null &&
                transaction.getToAccount().getOwner().getUsername().equals(username);

        return isSender || isReceiver;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void performSystemTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String memo) {

        Account fromAccount = findAndLockAccountById(fromAccountId);
        Account toAccount = findAndLockAccountById(toAccountId);

        if (fromAccount.getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User account is not active.");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds.");
        }

        BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
        fromAccount.setBalance(newFromBalance);
        BigDecimal newToBalance = toAccount.getBalance().add(amount);
        toAccount.setBalance(newToBalance);

        Transaction transactionLog = new Transaction();
        transactionLog.setTransactionType(TransactionType.TRANSFER);
        transactionLog.setStatus(TransactionStatus.COMPLETED);
        transactionLog.setFromAccount(fromAccount);
        transactionLog.setToAccount(toAccount);
        transactionLog.setAmount(amount);
        transactionLog.setTransactionDate(LocalDateTime.now());
        transactionLog.setDescription("Scheduled Transfer to " + toAccount.getOwner().getFirstName());
        transactionLog.setUserMemo(memo);
        transactionLog.setRunningBalance(newFromBalance);
        em.persist(transactionLog);
    }

    @Override
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void payBill(String username, BillPaymentRequestDTO request) {

        User user = findUserByUsername(username);
        Account fromAccount = findAndLockAccount(request.getFromAccountNumber());

        Biller biller = findBillerById(request.getBillerId());
        Account toBillerAccount = findAndLockAccount(biller.getInternalAccount().getAccountNumber());
        BigDecimal paymentAmount = request.getAmount();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Your account is not active.");
        }
        if (!fromAccount.getOwner().equals(user)) {
            throw new SecurityException("Authorization error: You do not own the source account.");
        }
        if (biller.getStatus() != BillerStatus.ACTIVE) {
            throw new IllegalStateException("This biller is not currently active.");
        }
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds.");
        }


        BigDecimal newFromBalance = fromAccount.getBalance().subtract(paymentAmount);
        fromAccount.setBalance(newFromBalance);

        BigDecimal newToBalance = toBillerAccount.getBalance().add(paymentAmount);
        toBillerAccount.setBalance(newToBalance);
        Transaction log = new Transaction();
        log.setTransactionType(TransactionType.BILL_PAYMENT);
        log.setStatus(TransactionStatus.COMPLETED);
        log.setFromAccount(fromAccount);
        log.setToAccount(toBillerAccount);
        log.setAmount(request.getAmount());
        log.setTransactionDate(LocalDateTime.now());
        log.setDescription("Bill payment to " + biller.getBillerName());
        log.setUserMemo("Ref: " + request.getBillerReferenceNumber() + " - " + request.getUserMemo());
        log.setRunningBalance(fromAccount.getBalance());

        em.persist(log);
    }

    private Biller findBillerById(Long billerId) {
        Biller biller = em.find(Biller.class, billerId);
        if (biller == null) {
            throw new IllegalArgumentException("Biller with ID " + billerId + " not found.");
        }
        return biller;
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public List<TransactionDTO> getTransactionHistory(
            String username, String accountNumber, LocalDate startDate, LocalDate endDate,
            TransactionType transactionType, int pageNumber, int pageSize) {


        Account account = findAccountByNumber(accountNumber);
        if (account.getOwner() == null || !account.getOwner().getUsername().equals(username)) {
            throw new SecurityException("Authorization error: You do not own this account.");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT t FROM Transaction t WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId)");


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


        TypedQuery<Transaction> query = em.createQuery(jpql.toString(), Transaction.class);


        for (java.util.Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }


        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList().stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }

    private Account findAndLockAccountById(Long accountId) {
        try {
            return em.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Account with ID '" + accountId + "' not found.");
        }
    }

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

    @Override
    public Optional<Biller> findBillerByInternalAccount(Account account) {
        if (account == null || account.getAccountType() != AccountType.BILLER) {
            return Optional.empty();
        }
        try {
            TypedQuery<Biller> query = em.createQuery(
                    "SELECT b FROM Biller b WHERE b.internalAccount = :account", Biller.class);
            query.setParameter("account", account);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}