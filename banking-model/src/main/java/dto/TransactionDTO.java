package dto;


import entity.Transaction;
import enums.TransactionStatus;
import enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for representing a completed transaction in an API response (e.g., for transaction history).
 */
public class TransactionDTO {
    private Long id;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private String fromAccountNumber;
    private String toAccountNumber;
    private LocalDateTime transactionDate;
    private String description;
    private String userMemo;

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

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
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

    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.transactionType = transaction.getTransactionType();
        this.status = transaction.getStatus();
        this.amount = transaction.getAmount();
        this.transactionDate = transaction.getTransactionDate();
        this.description = transaction.getDescription();
        this.userMemo = transaction.getUserMemo();

        // Safely get account numbers
        if (transaction.getFromAccount() != null) {
            this.fromAccountNumber = transaction.getFromAccount().getAccountNumber();
        }
        if (transaction.getToAccount() != null) {
            this.toAccountNumber = transaction.getToAccount().getAccountNumber();
        }
    }

}