package dto;

import enums.TransactionStatus;
import enums.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A simple DTO to hold the full details of a single transaction.
 * This class has no business logic; it is just a data container.
 */
public class TransactionDetailDTO {

    private Long id;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private LocalDateTime transactionDate;

    // Sender Details
    private String fromAccountNumber;
    private String fromOwnerName;

    // Receiver Details
    private String toAccountNumber;
    private String toOwnerName;
    private String fromOwnerAvatarUrl;
    private String toOwnerAvatarUrl;

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

    public String getFromOwnerName() {
        return fromOwnerName;
    }

    public void setFromOwnerName(String fromOwnerName) {
        this.fromOwnerName = fromOwnerName;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public String getToOwnerName() {
        return toOwnerName;
    }

    public void setToOwnerName(String toOwnerName) {
        this.toOwnerName = toOwnerName;
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

    public String getFromOwnerAvatarUrl() {
        return fromOwnerAvatarUrl;
    }

    public void setFromOwnerAvatarUrl(String fromOwnerAvatarUrl) {
        this.fromOwnerAvatarUrl = fromOwnerAvatarUrl;
    }

    public String getToOwnerAvatarUrl() {
        return toOwnerAvatarUrl;
    }

    public void setToOwnerAvatarUrl(String toOwnerAvatarUrl) {
        this.toOwnerAvatarUrl = toOwnerAvatarUrl;
    }



    // A no-argument constructor is required.
    public TransactionDetailDTO() {}


}