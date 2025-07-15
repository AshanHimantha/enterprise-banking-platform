package service;


import entity.Account;
import entity.InterestRate;
import entity.InterestRateId;
import entity.Transaction;
import enums.AccountType;
import enums.TransactionStatus;
import enums.TransactionType;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
public class InterestServiceImpl implements InterestService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void accrueDailyInterestForAllEligibleAccounts() {
        // 1. Fetch all defined interest rates into an efficient Map for lookup.
        Map<InterestRateId, BigDecimal> rateMap = getInterestRateMap();

        if (rateMap.isEmpty()) {
            System.err.println("INTEREST ACCRUAL: No interest rates found in the database. Aborting job.");
            return;
        }

        // 2. Find all accounts eligible for interest accrual.
        List<Account> accounts = findEligibleAccountsForAccrual();
        System.out.println("INTEREST ACCRUAL: Found " + accounts.size() + " eligible accounts.");

        for (Account account : accounts) {
            // 3. For each account, create a composite key to find its specific rate.
            InterestRateId keyToFind = new InterestRateId(
                    account.getAccountType(),
                    account.getOwner().getAccountLevel()
            );

            // 4. Look up the rate in the map.
            BigDecimal annualRate = rateMap.get(keyToFind);

            if (annualRate != null) {
                // 5. If a rate is found, calculate and accrue the daily interest.
                BigDecimal dailyRate = annualRate.divide(DAYS_IN_YEAR, 12, RoundingMode.HALF_UP);
                BigDecimal dailyInterest = account.getBalance().multiply(dailyRate);

                account.setAccruedInterest(account.getAccruedInterest().add(dailyInterest));
                em.merge(account);
            } else {
                // Log a warning if no specific rate is configured for this combination.
                System.err.println("WARN: No interest rate found for combination: "
                        + account.getAccountType() + "/" + account.getOwner().getAccountLevel()
                        + " for account " + account.getAccountNumber());
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void payoutInterestForAllEligibleAccounts() {
        List<Account> accountsToPay = findAccountsWithAccruedInterest();
        System.out.println("INTEREST PAYOUT: Found " + accountsToPay.size() + " accounts to pay out.");

        for (Account account : accountsToPay) {
            // Round the final payout amount to 4 decimal places for the balance.
            BigDecimal payoutAmount = account.getAccruedInterest().setScale(4, RoundingMode.DOWN);

            if (payoutAmount.compareTo(BigDecimal.ZERO) > 0) {
                account.setBalance(account.getBalance().add(payoutAmount));
                account.setAccruedInterest(BigDecimal.ZERO); // Reset for the next period.
                em.merge(account);

                // Log the payout as a transaction.
                Transaction log = new Transaction();
                log.setTransactionType(TransactionType.INTEREST_PAYOUT);
                log.setStatus(TransactionStatus.COMPLETED);
                log.setToAccount(account);
                log.setAmount(payoutAmount);
                log.setDescription("Monthly Interest Payout");
                log.setTransactionDate(LocalDateTime.now());
                log.setRunningBalance(account.getBalance());
                em.persist(log);
            }
        }
    }

    // --- Helper Methods ---

    private Map<InterestRateId, BigDecimal> getInterestRateMap() {
        return em.createQuery("SELECT ir FROM InterestRate ir", InterestRate.class)
                .getResultStream()
                .collect(Collectors.toMap(InterestRate::getId, InterestRate::getAnnualRate));
    }

    private List<Account> findEligibleAccountsForAccrual() {
        return em.createQuery(
                        "SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountType IN (:saving, :current)", Account.class)
                .setParameter("saving", AccountType.SAVING)
                .setParameter("current", AccountType.CURRENT)
                .getResultList();
    }

    private List<Account> findAccountsWithAccruedInterest() {
        return em.createQuery("SELECT a FROM Account a WHERE a.accruedInterest > 0", Account.class)
                .getResultList();
    }
}