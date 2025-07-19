package entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit")
public class Deposit implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The transaction record for this deposit. Creates a one-to-one link.
    @OneToOne
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    // The account that received the deposit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;

    // The employee who processed the deposit. Stored as username for auditing.
    @Column(nullable = false)
    private String processedByEmployee;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public String getProcessedByEmployee() {
        return processedByEmployee;
    }

    public void setProcessedByEmployee(String processedByEmployee) {
        this.processedByEmployee = processedByEmployee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDepositTimestamp() {
        return depositTimestamp;
    }

    public void setDepositTimestamp(LocalDateTime depositTimestamp) {
        this.depositTimestamp = depositTimestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // The amount deposited.
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime depositTimestamp;

    // An optional note, e.g., "Cash deposit at Main Branch".
    private String notes;


}