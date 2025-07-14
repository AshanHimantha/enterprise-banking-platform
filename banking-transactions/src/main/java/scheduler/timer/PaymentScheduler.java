package scheduler.timer;


import dto.TransactionRequestDTO;
import entity.Account;
import entity.ScheduledPayment;
import entity.Transaction;
import enums.TransactionStatus;
import enums.TransactionType;
import enums.UserStatus;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import scheduler.ScheduledPaymentService;
import service.TransactionService;

import java.time.LocalDateTime;
import java.util.List;


@Singleton
@Startup
public class PaymentScheduler {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private ScheduledPaymentService scheduledPaymentService;

    @EJB
    private TransactionService transactionService; // Still needed for user-to-user transfers

    /**
     * This method is automatically invoked by the EJB container.
     * Runs every day at 3:00 AM Colombo time.
     */
    @Schedule(hour = "4", minute = "31", second = "0", persistent = true, timezone = "Asia/Colombo")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) // Each scheduler run is its own new transaction
    public void executeDuePayments() {
        System.out.println("SCHEDULER: Starting job run...");

        System.out.println("PaymentScheduler starting job at " + java.time.LocalDateTime.now());
        List<ScheduledPayment> duePayments = scheduledPaymentService.findDuePayments();

        if (duePayments.isEmpty()) {
            System.out.println("No due payments to process today.");
            return;
        }

        System.out.println("Found " + duePayments.size() + " due payment(s) to process.");

        for (ScheduledPayment payment : duePayments) {
            try {
                // Determine if it's a bill payment or a user transfer
                if (payment.getBiller() != null) {
                    processScheduledBillPayment(payment);
                } else if (payment.getToAccount() != null) {
                    processScheduledUserTransfer(payment);
                }

                // If successful, reschedule for the next period
                scheduledPaymentService.reschedulePayment(payment);
                System.out.println("  -> Successfully processed and rescheduled payment ID: " + payment.getId());

            } catch (Exception e) {
                System.err.println("  -> FAILED to process scheduled payment ID " + payment.getId() + ". Reason: " + e.getMessage());
                scheduledPaymentService.markPaymentAsFailed(payment, e.getMessage());
            }
        }
        System.out.println("PaymentScheduler job finished at " + java.time.LocalDateTime.now());
    }


    private void processScheduledUserTransfer(ScheduledPayment payment) {
        System.out.println("  -> Type: User Transfer to account " + payment.getToAccount().getAccountNumber());

        transactionService.performSystemTransfer(
                payment.getFromAccount().getId(),
                payment.getToAccount().getId(),
                payment.getAmount(),
                "Recurring: " + (payment.getUserMemo() != null ? payment.getUserMemo() : "")
        );
    }

    private void processScheduledBillPayment(ScheduledPayment payment) {
        System.out.println("  -> Type: Bill Payment to " + payment.getBiller().getBillerName());

        // Lock accounts to prevent race conditions
        Account fromAccount = em.find(Account.class, payment.getFromAccount().getId(), LockModeType.PESSIMISTIC_WRITE);
        Account toBillerAccount = em.find(Account.class, payment.getBiller().getInternalAccount().getId(), LockModeType.PESSIMISTIC_WRITE);

        // Perform validations
        if (fromAccount.getOwner().getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User account is not active.");
        }
        if (fromAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds.");
        }

        // Perform debit and credit
        fromAccount.setBalance(fromAccount.getBalance().subtract(payment.getAmount()));
        toBillerAccount.setBalance(toBillerAccount.getBalance().add(payment.getAmount()));

        // Log the transaction
        Transaction log = new Transaction();
        log.setTransactionType(TransactionType.BILL_PAYMENT);
        log.setStatus(TransactionStatus.COMPLETED);
        log.setFromAccount(fromAccount);
        log.setToAccount(toBillerAccount);
        log.setAmount(payment.getAmount());
        log.setTransactionDate(LocalDateTime.now());
        log.setDescription("Scheduled bill payment to " + payment.getBiller().getBillerName());
        log.setUserMemo("Ref: " + payment.getBillerReferenceNumber() + " - " + payment.getUserMemo());
        log.setRunningBalance(fromAccount.getBalance());

        em.persist(log);
    }
}