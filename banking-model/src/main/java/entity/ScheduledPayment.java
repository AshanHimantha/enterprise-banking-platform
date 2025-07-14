package entity;


import enums.PaymentFrequency;
import enums.ScheduledPaymentStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a user's instruction to make a recurring payment.
 * This can be for a user-to-user transfer or a bill payment.
 */
@Entity
@Table(name = "scheduled_payment")
public class ScheduledPayment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who owns this scheduled payment rule.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The account from which funds will be debited.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;

    // --- Destination: Only ONE of these will be populated ---

    // For user-to-user transfers, this will be the recipient's account.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = true)
    private Account toAccount;

    // For bill payments, this links to the Biller entity from the directory.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_id", nullable = true)
    private Biller biller;

    // For bill payments, this stores the user's specific account/reference number for that biller.
    @Column(length = 100, nullable = true)
    private String billerReferenceNumber;

    // --- Scheduling and Payment Details ---

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency frequency;

    @Column(nullable = false)
    private LocalDate nextExecutionDate;

    @Column(nullable = false)
    private LocalDate startDate;

    // An optional date after which the schedule will no longer run.
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledPaymentStatus status;

    // An optional note provided by the user for the transactions.
    @Column(length = 255)
    private String userMemo;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public Biller getBiller() {
        return biller;
    }

    public void setBiller(Biller biller) {
        this.biller = biller;
    }

    public String getBillerReferenceNumber() {
        return billerReferenceNumber;
    }

    public void setBillerReferenceNumber(String billerReferenceNumber) {
        this.billerReferenceNumber = billerReferenceNumber;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
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