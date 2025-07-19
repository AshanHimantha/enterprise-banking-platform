package service;

import dto.InterestRateDTO;
import entity.*;
import enums.AccountType;
import enums.TransactionStatus;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class InterestServiceImpl implements InterestService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void accrueDailyInterestForAllEligibleAccounts() {
        Map<InterestRateId, BigDecimal> rateMap = getInterestRateMap();
        final BigDecimal daysInCurrentYear = new BigDecimal(Year.now().length());
        List<Account> accounts = findEligibleAccountsForAccrual();

        LocalDate today = LocalDate.now();
        System.out.println("INTEREST SERVICE: Accruing interest for " + accounts.size() + " eligible accounts on " + today);

        for (Account account : accounts) {
            // Check if interest for today has already been calculated for this account to prevent duplicates
            if (hasAccruedToday(account, today)) {
                continue; // Skip to the next account
            }

            InterestRateId keyToFind = new InterestRateId(account.getAccountType(), account.getOwner().getAccountLevel());
            BigDecimal annualRate = rateMap.get(keyToFind);

            if (annualRate != null) {
                BigDecimal dailyInterest = account.getBalance().multiply(annualRate)
                        .divide(daysInCurrentYear, 20, RoundingMode.HALF_UP);

                InterestAccrual accrualRecord = new InterestAccrual();
                accrualRecord.setAccount(account);
                accrualRecord.setAccrualDate(today);
                accrualRecord.setInterestAmount(dailyInterest);
                accrualRecord.setClosingBalance(account.getBalance()); // Store the balance for auditing
                accrualRecord.setAnnualRateUsed(annualRate);
                accrualRecord.setPaidOut(false);


                em.persist(accrualRecord); // Save the new daily record
            }
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void payoutInterestForAllEligibleAccounts() {
        // Find all accounts that have unpaid accruals
        List<Account> accountsToPay = findAccountsWithUnpaidAccruals();

        for (Account account : accountsToPay) {
            // 1. Calculate the total interest to be paid by SUMMING the unpaid records.
            BigDecimal totalPayout = calculateTotalUnpaidInterestFor(account);

            BigDecimal payoutAmount = totalPayout.setScale(4, RoundingMode.DOWN);
            System.out.println("INTEREST SERVICE: Payout for account " + account.getAccountNumber() + " is " + payoutAmount);

            if (payoutAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 2. Add interest to the main balance
                account.setBalance(account.getBalance().add(payoutAmount));


                em.merge(account);
                System.out.println("INTEREST SERVICE: Updated balance for account " + account.getAccountNumber() + " to " + account.getBalance());

                Transaction log = new Transaction();
                log.setTransactionType(TransactionType.INTEREST_PAYOUT);
                log.setStatus(TransactionStatus.COMPLETED);
                log.setFromAccount(null); // Interest comes from the bank, not another account.
                log.setToAccount(account); // It is credited TO the user's account.
                log.setAmount(payoutAmount);
                log.setDescription("Monthly Interest Payout");
                log.setUserMemo(null); // No user memo for a system transaction.
                log.setTransactionDate(LocalDateTime.now());
                log.setRunningBalance(account.getBalance()); // Store the new balance after the payout.

                em.persist(log); // Save the new transaction record to the database.

                markAccrualsAsPaidFor(account);
            }
        }
    }


    @Override
    public List<InterestRateDTO> getAllInterestRates() {
        return em.createQuery("SELECT ir FROM InterestRate ir", InterestRate.class)
                .getResultStream()
                .map(InterestRateDTO::new)
                .collect(Collectors.toList());
    }


    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public InterestRateDTO saveOrUpdateInterestRate(InterestRateDTO rateDTO) {
        // Validate input
        if (rateDTO.getAccountType() == null || rateDTO.getAccountLevel() == null || rateDTO.getAnnualRate() == null) {
            throw new IllegalArgumentException("AccountType, AccountLevel, and AnnualRate are required.");
        }
        if (rateDTO.getAnnualRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Annual rate cannot be negative.");
        }

        // Create the composite key
        InterestRateId id = new InterestRateId(rateDTO.getAccountType(), rateDTO.getAccountLevel());

        // Try to find an existing rate
        InterestRate rateEntity = em.find(InterestRate.class, id);

        if (rateEntity == null) {
            // If it doesn't exist, create a new one
            rateEntity = new InterestRate();
            rateEntity.setId(id);
        }

        // Update the entity with data from the DTO
        rateEntity.setAnnualRate(rateDTO.getAnnualRate());
        rateEntity.setDescription(rateDTO.getDescription());

        // Use merge() to either insert the new entity or update the existing one.
        InterestRate savedEntity = em.merge(rateEntity);

        return new InterestRateDTO(savedEntity);
    }



    private boolean hasAccruedToday(Account account, LocalDate date) {
        Long count = em.createQuery("SELECT COUNT(ia) FROM InterestAccrual ia WHERE ia.account = :account AND ia.accrualDate = :date", Long.class)
                .setParameter("account", account)
                .setParameter("date", date)
                .getSingleResult();
        return count > 0;
    }

    private List<Account> findAccountsWithUnpaidAccruals() {
        // This query finds the DISTINCT accounts that have at least one unpaid accrual record.
        return em.createQuery("SELECT DISTINCT ia.account FROM InterestAccrual ia WHERE ia.paidOut = false", Account.class)
                .getResultList();
    }

    private BigDecimal calculateTotalUnpaidInterestFor(Account account) {
        // This query SUMS up all the highly precise interest amounts.
        TypedQuery<BigDecimal> query = em.createQuery(
                "SELECT SUM(ia.interestAmount) FROM InterestAccrual ia WHERE ia.account = :account AND ia.paidOut = false", BigDecimal.class);
        query.setParameter("account", account);
        BigDecimal total = query.getSingleResult();
        return total != null ? total : BigDecimal.ZERO;
    }

    private void markAccrualsAsPaidFor(Account account) {
        // This query marks all unpaid records as paid in a single, efficient UPDATE statement.
        em.createQuery("UPDATE InterestAccrual ia SET ia.paidOut = true WHERE ia.account = :account AND ia.paidOut = false")
                .setParameter("account", account)
                .executeUpdate();
    }

    private Map<InterestRateId, BigDecimal> getInterestRateMap() {
        return em.createQuery("SELECT ir FROM InterestRate ir", InterestRate.class)
                .getResultStream()
                .collect(Collectors.toMap(InterestRate::getId, InterestRate::getAnnualRate));
    }

    private List<Account> findEligibleAccountsForAccrual() {
        // Exclude accounts with non-positive balances from interest calculation
        return em.createQuery(
                        "SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountType IN (:saving, :current) AND a.balance > 0", Account.class)
                .setParameter("saving", AccountType.SAVING)
                .setParameter("current", AccountType.CURRENT)
                .getResultList();
    }


}