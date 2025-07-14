package service;


import dto.TransactionRequestDTO;
import entity.Account;
import entity.Transaction;
import entity.User;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class TransactionServiceImpl implements TransactionService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void performTransfer(String username, TransactionRequestDTO request) {
        // 1. Find the user who is making the request.
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult();

        // Passcode verification step is REMOVED.

        // 2. Find and lock the accounts to prevent race conditions.
        Account fromAccount = findAndLockAccount(request.getFromAccountNumber());
        Account toAccount = findAndLockAccount(request.getToAccountNumber());

        // 3. Perform security and business rule validations.
        if (!fromAccount.getOwner().equals(user)) {
            throw new SecurityException("Authorization failed: You do not own the source account.");
        }
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds for the transfer.");
        }
        // Future validations (account status, daily limits) would go here.

        // 4. Perform the debit and credit.
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));

        // 5. Create a transaction log record.
        Transaction transactionLog = new Transaction();
        transactionLog.setFromAccount(fromAccount);
        transactionLog.setToAccount(toAccount);
        transactionLog.setAmount(request.getAmount());
        transactionLog.setTransactionDate(LocalDateTime.now());

        em.persist(transactionLog);
    }

    // Helper method to find and lock an account for update.
    private Account findAndLockAccount(String accountNumber) {
        TypedQuery<Account> query = em.createQuery("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber", Account.class);
        query.setParameter("accountNumber", accountNumber);

        // Set a pessimistic write lock on the row in the database.
        // This prevents other transactions from modifying this account until this transaction is complete.
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);

        try {
            return query.getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            throw new IllegalArgumentException("Account with number " + accountNumber + " not found.");
        }
    }
}