package dto;

import entity.Transaction;
import enums.TransactionStatus;
import enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A detailed Data Transfer Object for viewing transactions from an administrative perspective.
 * This DTO is designed to be populated by a service layer that can resolve
 * all necessary details, like owner usernames and biller names.
 */
public class AdminTransactionDTO {

    private Long id;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime transactionDate;

    // Sender Details
    private String fromAccountNumber;
    private String fromOwnerUsername;

    // Receiver Details
    private String toAccountNumber;
    private String toOwnerUsername; // Can hold a username OR a Biller's name

    private String description;
    private String userMemo;
    private BigDecimal runningBalance;


    public AdminTransactionDTO() {
    }


    public AdminTransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.amount = transaction.getAmount();
        this.transactionDate = transaction.getTransactionDate();
        this.description = transaction.getDescription();
        this.userMemo = transaction.getUserMemo();
        this.runningBalance = transaction.getRunningBalance();

        // Map "From" details
        if (transaction.getFromAccount() != null) {
            this.fromAccountNumber = transaction.getFromAccount().getAccountNumber();
            if (transaction.getFromAccount().getOwner() != null) {
                this.fromOwnerUsername = transaction.getFromAccount().getOwner().getUsername();
            }
        }

        // Map "To" details
        if (transaction.getToAccount() != null) {
            this.toAccountNumber = transaction.getToAccount().getAccountNumber();
            if (transaction.getToAccount().getOwner() != null) {
                this.toOwnerUsername = transaction.getToAccount().getOwner().getUsername();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getFromOwnerUsername() {
        return fromOwnerUsername;
    }

    public void setFromOwnerUsername(String fromOwnerUsername) {
        this.fromOwnerUsername = fromOwnerUsername;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public String getToOwnerUsername() {
        return toOwnerUsername;
    }

    public void setToOwnerUsername(String toOwnerUsername) {
        this.toOwnerUsername = toOwnerUsername;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserMemo() {
        return userMemo;
    }

    public void setUserMemo(String userMemo) {
        this.userMemo = userMemo;
    }

    public BigDecimal getRunningBalance() {
        return runningBalance;
    }

    public void setRunningBalance(BigDecimal runningBalance) {
        this.runningBalance = runningBalance;
    }
}