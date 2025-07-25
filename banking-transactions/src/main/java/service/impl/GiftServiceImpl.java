package service.impl;

import annotation.Audit;
import annotation.Logging;
import entity.Account;
import entity.Transaction;
import entity.User;
import enums.AccountType;
import enums.TransactionStatus;
import enums.TransactionType;
import enums.UserStatus;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.GiftService;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Audit
@Logging
@Stateless
@Interceptors(LoggingInterceptor.class)
public class GiftServiceImpl implements GiftService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;


    private static final BigDecimal GIFT_AMOUNT = new BigDecimal("100.00");

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public BigDecimal claimWelcomeGift(String username) {
        // 1. Find the user
        User user = findUserByUsername(username);

        // 2. Enforce Business Rules
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Your account is not active yet. Please complete KYC.");
        }
        if (user.isHasClaimedWelcomeGift()) {
            throw new IllegalStateException("Welcome gift has already been claimed for this account.");
        }

        // 3. Find a suitable account to deposit the gift into (e.g., the first SAVING or CURRENT account)
        Account targetAccount = findPrimaryDepositAccountForUser(user);

        // 4. Update balances and user status
        targetAccount.setBalance(targetAccount.getBalance().add(GIFT_AMOUNT));
        user.setHasClaimedWelcomeGift(true);

        em.merge(targetAccount);
        em.merge(user);

        // 5. Log the transaction
        Transaction log = new Transaction();
        log.setTransactionType(TransactionType.GIFT); // Or a new "GIFT" type
        log.setStatus(TransactionStatus.COMPLETED);
        log.setFromAccount(null); // Gift comes from the "bank"
        log.setToAccount(targetAccount);
        log.setAmount(GIFT_AMOUNT);
        log.setDescription("Welcome Gift Claimed");
        log.setTransactionDate(LocalDateTime.now());
        log.setRunningBalance(targetAccount.getBalance());
        em.persist(log);

        return targetAccount.getBalance();
    }



    private Account findPrimaryDepositAccountForUser(User user) {
        try {
            // Prioritize SAVING account, then CURRENT account
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a WHERE a.owner = :user AND a.accountType IN (:saving, :current) ORDER BY a.accountType ASC", Account.class);
            query.setParameter("user", user);
            query.setParameter("saving", AccountType.SAVING);
            query.setParameter("current", AccountType.CURRENT);
            query.setMaxResults(1);
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalStateException("No eligible SAVING or CURRENT account found to deposit the gift.");
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
}