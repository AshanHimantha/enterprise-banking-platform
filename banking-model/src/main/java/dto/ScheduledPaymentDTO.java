package dto;

import entity.ScheduledPayment;
import enums.PaymentFrequency;
import enums.ScheduledPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A Data Transfer Object for representing a user's scheduled payment in an API response.
 * This class translates the database entity into a client-friendly JSON object.
 */
public class ScheduledPaymentDTO {

    private Long id;
    private String fromAccountNumber;
    private String toAccountNumber;
    private String toRecipientName; // A friendly name for the destination
    private BigDecimal amount;
    private PaymentFrequency frequency;
    private LocalDate nextExecutionDate;
    private LocalDate endDate;
    private ScheduledPaymentStatus status;
    private String userMemo;

    /**
     * A public, no-argument constructor.
     * This is REQUIRED by frameworks like JPA, JAX-RS (for JSON deserialization),
     * and CDI for creating instances of this class via reflection.
     */
    public ScheduledPaymentDTO() {
    }

    /**
     * A constructor to easily map from a ScheduledPayment entity to this DTO.
     * This is where the logic to create a user-friendly response lives.
     *
     * @param schedule The ScheduledPayment entity from the database.
     */
    public ScheduledPaymentDTO(ScheduledPayment schedule) {
        this.id = schedule.getId();
        this.amount = schedule.getAmount();
        this.frequency = schedule.getFrequency();
        this.nextExecutionDate = schedule.getNextExecutionDate();
        this.endDate = schedule.getEndDate();
        this.status = schedule.getStatus();
        this.userMemo = schedule.getUserMemo();

        if (schedule.getFromAccount() != null) {
            this.fromAccountNumber = schedule.getFromAccount().getAccountNumber();
        }

        // Determine the recipient's information based on whether it's a
        // user-to-user transfer or a bill payment.
        if (schedule.getToAccount() != null) {
            this.toAccountNumber = schedule.getToAccount().getAccountNumber();
            if (schedule.getToAccount().getOwner() != null) {
                this.toRecipientName = schedule.getToAccount().getOwner().getFirstName() + " " + schedule.getToAccount().getOwner().getLastName();
            } else if (schedule.getToAccount().getAccountType() != null && schedule.getToAccount().getAccountType().name().equals("BILLER")) {
                // This is a fallback in case the Biller name can't be found directly
                this.toRecipientName = "Biller Payment";
            }
        } else if (schedule.getBiller() != null) {
            // This logic is for when you implement bill payments
            this.toAccountNumber = schedule.getBiller().getInternalAccount().getAccountNumber();
            this.toRecipientName = schedule.getBiller().getBillerName();
        }
    }

    // --- Getters and Setters for all fields ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public String getToRecipientName() {
        return toRecipientName;
    }

    public void setToRecipientName(String toRecipientName) {
        this.toRecipientName = toRecipientName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(PaymentFrequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getNextExecutionDate() {
        return nextExecutionDate;
    }

    public void setNextExecutionDate(LocalDate nextExecutionDate) {
        this.nextExecutionDate = nextExecutionDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ScheduledPaymentStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduledPaymentStatus status) {
        this.status = status;
    }

    public String getUserMemo() {
        return userMemo;
    }

    public void setUserMemo(String userMemo) {
        this.userMemo = userMemo;
    }
}