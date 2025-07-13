package service;

import entity.Account;
import entity.User;
import enums.AccountType;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

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

    /**
     * Generates a human-readable account number in the format: BANK-YYYY-XXXXXX
     * Example: BANK-2025-123456
     *
     * Format breakdown:
     * - BANK: Bank identifier prefix
     * - YYYY: Current year
     * - XXXXXX: 6-digit sequential number
     */
    private String generateHumanReadableAccountNumber() {
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

    /**
     * Generates a 6-digit sequential number starting from 100001
     * This ensures account numbers are easy to read and share
     */
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