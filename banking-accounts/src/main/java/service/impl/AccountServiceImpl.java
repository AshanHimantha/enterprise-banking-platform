package service.impl;

import dto.CreateAccountDTO;
import entity.Account;
import dto.DashboardAccountDTO;
import entity.User;
import enums.AccountType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.AccountService;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class AccountServiceImpl implements AccountService { // Implements the interface

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;



    @Override // Add the @Override annotation
    public void createAccountForNewUser(User user, BigDecimal initialDeposit , AccountType accountType) {
        em.persist(user);

        Account account = new Account();
        account.setOwner(user);
        account.setBalance(initialDeposit);
        account.setAccountNumber(generateHumanReadableAccountNumber());
        account.setAccountType(accountType);

        em.persist(account);

        System.out.println("Successfully created user: " + user.getUsername() + " and account: " + account.getAccountNumber());
    }


    @Override
    @RolesAllowed("CUSTOMER")
    public List<DashboardAccountDTO> findAccountsByUsername(String username) {
        TypedQuery<Account> query = em.createQuery(
                "SELECT a FROM Account a WHERE a.owner.username = :username ORDER BY a.accountType", Account.class);
        query.setParameter("username", username);

        return query.getResultList().stream()
                .map(DashboardAccountDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public Optional<DashboardAccountDTO> findAccountByNumberForUser(String accountNumber, String username) {
        TypedQuery<Account> query = em.createQuery(
                "SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.owner.username = :username", Account.class);
        query.setParameter("accountNumber", accountNumber);
        query.setParameter("username", username);

        try {
            Account account = query.getSingleResult();
            return Optional.of(new DashboardAccountDTO(account));
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public void verifyAccountOwnership(String username, String accountNumber) {
        // Basic validation for the input parameters.
        if (username == null || username.trim().isEmpty() || accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new SecurityException("Authentication details are missing or invalid.");
        }


        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.accountNumber = :accountNumber AND a.owner.username = :username", Long.class);

        query.setParameter("accountNumber", accountNumber);
        query.setParameter("username", username);


        if (query.getSingleResult() == 0) {
            throw new SecurityException("Access denied to the requested resource.");
        }


    }


    @Override
    @RolesAllowed("CUSTOMER")
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DashboardAccountDTO createNewAccountForUser(String username, CreateAccountDTO dto) {
        // --- 1. Validation ---
        AccountType requestedType = dto.getAccountType();
        if (requestedType != AccountType.SAVING && requestedType != AccountType.CURRENT) {
            throw new IllegalArgumentException("Account creation is only allowed for SAVING or CURRENT types.");
        }

        User user = findUserByUsername(username); // You'll need this helper method

        // --- 2. Enforce Business Rule: Account Limits ---
        long existingCount = countAccountsOfTypeForUser(user, requestedType);

        final long MAX_ACCOUNTS_PER_TYPE = 2;
        if (existingCount >= MAX_ACCOUNTS_PER_TYPE) {
            throw new IllegalStateException("You have reached the maximum limit of " + MAX_ACCOUNTS_PER_TYPE + " " + requestedType + " accounts.");
        }

        // --- 3. Create the New Account ---
        Account newAccount = new Account();
        newAccount.setOwner(user);
        newAccount.setAccountType(requestedType);
        newAccount.setBalance(BigDecimal.ZERO);
        newAccount.setAccountNumber(generateHumanReadableAccountNumber());

        em.persist(newAccount);

        // 4. Return a DTO of the new account
        return new DashboardAccountDTO(newAccount);
    }

    // --- NEW HELPER METHODS ---

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User not found.");
        }
    }

    private long countAccountsOfTypeForUser(User user, AccountType accountType) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.owner = :user AND a.accountType = :type", Long.class);
        query.setParameter("user", user);
        query.setParameter("type", accountType);
        return query.getSingleResult();
    }

    @Override
    public Account findAccountByNumber(String accountNumber) {
        try {
            // Use JOIN FETCH to efficiently load the owner in the same query.
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class);
            query.setParameter("accountNumber", accountNumber);
            return query.getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            // Return null if no account is found with that number.
            return null;
        }
    }

    public String generateHumanReadableAccountNumber() {
        String bankPrefix = "ORBIN";
        String currentYear = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));


        String sequentialNumber = generateSequentialNumber();
        String accountNumber = bankPrefix + "-" + currentYear + "-" + sequentialNumber;

        // Ensure uniqueness by checking if account number already exists
        while (accountNumberExists(accountNumber)) {
            sequentialNumber = generateSequentialNumber();
            accountNumber = bankPrefix + "-" + currentYear + "-" + sequentialNumber;
        }

        return accountNumber;
    }


    private String generateSequentialNumber() {
        // Get the count of existing accounts and add base number
        long accountCount = getAccountCount();
        long nextNumber = 100001 + accountCount;

        // If we've exceeded 6 digits, use random 6-digit number
        if (nextNumber > 999999) {
            Random random = new Random();
            nextNumber = 100001 + random.nextInt(899999); // Random between 100001-999999
        }

        return String.format("%06d", nextNumber);
    }

    /**
     * Gets the total count of accounts in the system
     */
    private long getAccountCount() {
        try {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(a) FROM Account a", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            // If query fails, return 0 to start from base number
            return 0;
        }
    }

    /**
     * Checks if an account number already exists in the database
     */
    private boolean accountNumberExists(String accountNumber) {
        try {
            TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(a) FROM Account a WHERE a.accountNumber = :accountNumber", Long.class);
            query.setParameter("accountNumber", accountNumber);
            return query.getSingleResult() > 0;
        } catch (Exception e) {
            // If query fails, assume it doesn't exist
            return false;
        }
    }
}