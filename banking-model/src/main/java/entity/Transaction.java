package entity;


import enums.TransactionStatus;
import enums.TransactionType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // NEW: Status of the transaction

    @Column(nullable = false)
    private BigDecimal amount;

    // The account money came FROM. Null for top-ups.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id", nullable = true)
    private Account fromAccount;

    // The account money went TO. Null for some withdrawals/payments.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id", nullable = true)
    private Account toAccount;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    // System-generated description, e.g., "Transfer to Jane Doe"
    @Column(length = 255)
    private String description;

    // Optional note provided by the user, e.g., "For concert tickets"
    @Column(length = 255)
    private String userMemo;

    // Optional but recommended for performance
    @Column(precision = 19, scale = 4)
    private BigDecimal runningBalance;

    // --- Getters and Setters for all fields ---
    // (Generate these in your IDE)

    // id
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // transactionType
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }

    // status
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    // amount
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    // fromAccount
    public Account getFromAccount() { return fromAccount; }
    public void setFromAccount(Account fromAccount) { this.fromAccount = fromAccount; }

    // toAccount
    public Account getToAccount() { return toAccount; }
    public void setToAccount(Account toAccount) { this.toAccount = toAccount; }

    // transactionDate
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

    // description
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // userMemo
    public String getUserMemo() { return userMemo; }
    public void setUserMemo(String userMemo) { this.userMemo = userMemo; }

    // runningBalance
    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
}