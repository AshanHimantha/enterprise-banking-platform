package scheduler;

import annotation.Audit;
import annotation.Logging;
import dto.ScheduleRequestDTO;

import dto.ScheduledPaymentDTO;
import entity.Account;
import entity.Biller;
import entity.ScheduledPayment;
import entity.User;
import enums.BillerStatus;
import enums.PaymentFrequency;
import enums.ScheduledPaymentStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Audit
@Logging
@Stateless
@Interceptors(LoggingInterceptor.class)
public class ScheduledPaymentServiceImpl implements ScheduledPaymentService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed("CUSTOMER")
    public ScheduledPaymentDTO scheduleNewPayment(String username, ScheduleRequestDTO dto) {
        User user = findUserByUsername(username);
        Account fromAccount = findAccountByNumber(dto.getFromAccountNumber());

        // Security check: ensure the user owns the source account
        if (fromAccount.getOwner() == null || !fromAccount.getOwner().equals(user)) {
            throw new SecurityException("User does not own the source account.");
        }

        // Validate that a destination is provided and is not ambiguous
        boolean isUserTransfer = dto.getToAccountNumber() != null && !dto.getToAccountNumber().trim().isEmpty();
        boolean isBillPayment = dto.getBillerId() != null;

        if (isUserTransfer && isBillPayment) {
            throw new IllegalArgumentException("A scheduled payment can be for a user transfer OR a bill payment, but not both.");
        }
        if (!isUserTransfer && !isBillPayment) {
            throw new IllegalArgumentException("A destination (toAccountNumber or billerId) is required.");
        }

        ScheduledPayment newSchedule = new ScheduledPayment();
        newSchedule.setUser(user);
        newSchedule.setFromAccount(fromAccount);

        // Populate destination based on the type of schedule
        if (isBillPayment) {
            Biller biller = findBillerById(dto.getBillerId());
            if (biller.getStatus() != BillerStatus.ACTIVE) {
                throw new IllegalStateException("The selected biller is not currently active.");
            }
            if (dto.getBillerReferenceNumber() == null || dto.getBillerReferenceNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("A reference number (e.g., your account number with the biller) is required for bill payments.");
            }
            newSchedule.setBiller(biller);
            newSchedule.setBillerReferenceNumber(dto.getBillerReferenceNumber());
        } else { // isUserTransfer
            Account toAccount = findAccountByNumber(dto.getToAccountNumber());
            newSchedule.setToAccount(toAccount);
        }

        newSchedule.setAmount(dto.getAmount());
        newSchedule.setFrequency(dto.getFrequency());
        newSchedule.setStartDate(dto.getStartDate());
        newSchedule.setNextExecutionDate(dto.getStartDate());
        newSchedule.setEndDate(dto.getEndDate());
        newSchedule.setStatus(ScheduledPaymentStatus.ACTIVE);
        newSchedule.setUserMemo(dto.getUserMemo());

        em.persist(newSchedule);
        return new ScheduledPaymentDTO(newSchedule);
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public List<ScheduledPaymentDTO> getScheduledPaymentsForUser(String username) {
        TypedQuery<ScheduledPayment> query = em.createQuery(
                "SELECT s FROM ScheduledPayment s WHERE s.user.username = :username " +
                        "AND s.status IN (:activeStatus, :pausedStatus) ORDER BY s.nextExecutionDate", ScheduledPayment.class);
        query.setParameter("username", username);
        query.setParameter("activeStatus", ScheduledPaymentStatus.ACTIVE);
        query.setParameter("pausedStatus", ScheduledPaymentStatus.PAUSED);

        return query.getResultList().stream()
                .map(ScheduledPaymentDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public void pauseScheduledPayment(String username, Long scheduleId) {
        ScheduledPayment payment = findUserScheduleById(username, scheduleId);
        if (payment.getStatus() == ScheduledPaymentStatus.ACTIVE) {
            payment.setStatus(ScheduledPaymentStatus.PAUSED);
            em.merge(payment);
        } else {
            throw new IllegalStateException("Only active schedules can be paused.");
        }
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public void resumeScheduledPayment(String username, Long scheduleId) {
        ScheduledPayment payment = findUserScheduleById(username, scheduleId);
        if (payment.getStatus() == ScheduledPaymentStatus.PAUSED) {
            payment.setStatus(ScheduledPaymentStatus.ACTIVE);
            em.merge(payment);
        } else {
            throw new IllegalStateException("Only paused schedules can be resumed.");
        }
    }

    @Override
    @RolesAllowed("CUSTOMER")
    public void cancelScheduledPayment(String username, Long scheduleId) {
        ScheduledPayment payment = findUserScheduleById(username, scheduleId);
        em.remove(payment);
    }

    // --- Internal Methods for the EJB Timer ---

    @Override
    public List<ScheduledPayment> findDuePayments() {
        TypedQuery<ScheduledPayment> query = em.createQuery(
                "SELECT s FROM ScheduledPayment s WHERE s.status = :status AND s.nextExecutionDate <= :today", ScheduledPayment.class);
        query.setParameter("status", ScheduledPaymentStatus.ACTIVE);
        query.setParameter("today", LocalDate.now());
        return query.getResultList();
    }

    @Override
    public void reschedulePayment(ScheduledPayment payment) {
        LocalDate nextDate = calculateNextExecutionDate(payment.getNextExecutionDate(), payment.getFrequency());

        if (payment.getEndDate() != null && nextDate.isAfter(payment.getEndDate())) {
            payment.setStatus(ScheduledPaymentStatus.COMPLETED);
        } else {
            payment.setNextExecutionDate(nextDate);
        }
        em.merge(payment);
    }

    @Override
    public void markPaymentAsFailed(ScheduledPayment payment, String reason) {
        payment.setStatus(ScheduledPaymentStatus.FAILED);
        String memo = "Last attempt on " + LocalDate.now() + " failed: " + reason;
        payment.setUserMemo(memo);
        em.merge(payment);
    }

    // --- Helper Methods ---

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User with username '" + username + "' not found.");
        }
    }

    private Account findAccountByNumber(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber).getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Account with number '" + accountNumber + "' not found.");
        }
    }

    private Biller findBillerById(Long billerId) {
        Biller biller = em.find(Biller.class, billerId);
        if (biller == null) {
            throw new IllegalArgumentException("Biller with ID " + billerId + " not found.");
        }
        return biller;
    }

    private ScheduledPayment findUserScheduleById(String username, Long scheduleId) {
        try {
            return em.createQuery("SELECT s FROM ScheduledPayment s WHERE s.id = :scheduleId AND s.user.username = :username", ScheduledPayment.class)
                    .setParameter("scheduleId", scheduleId)
                    .setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityException("Scheduled payment not found or you do not have permission to access it.");
        }
    }

    private LocalDate calculateNextExecutionDate(LocalDate currentDate, PaymentFrequency frequency) {
        switch (frequency) {
            case DAILY: return currentDate.plusDays(1);
            case WEEKLY: return currentDate.plusWeeks(1);
            case MONTHLY: return currentDate.plusMonths(1);
            case QUARTERLY: return currentDate.plusMonths(3);
            case YEARLY: return currentDate.plusYears(1);
            default: throw new IllegalArgumentException("Unsupported frequency: " + frequency);
        }
    }
}