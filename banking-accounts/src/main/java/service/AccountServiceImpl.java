package service;

import entity.Account;
import entity.DashboardAccountDTO;
import entity.User;
import enums.AccountType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Stateless
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

    public String generateHumanReadableAccountNumber() {
        String bankPrefix = "ORBIN";
        String currentYear = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));

        // Generate a 6-digit sequential number
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