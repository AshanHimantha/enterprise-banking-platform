package dto;

import entity.Deposit;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public class DepositHistoryDTO {
    private Long depositId;
    private Long transactionId;
    private LocalDateTime timestamp;
    private BigDecimal amount;
    private String toAccountNumber;
    private String toAccountOwnerName;
    private String processedByEmployee; // The employee who made the deposit
    private String notes;

    public Long getDepositId() {
        return depositId;
    }

    public void setDepositId(Long depositId) {
        this.depositId = depositId;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public String getToAccountOwnerName() {
        return toAccountOwnerName;
    }

    public void setToAccountOwnerName(String toAccountOwnerName) {
        this.toAccountOwnerName = toAccountOwnerName;
    }

    public String getProcessedByEmployee() {
        return processedByEmployee;
    }

    public void setProcessedByEmployee(String processedByEmployee) {
        this.processedByEmployee = processedByEmployee;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public DepositHistoryDTO(Deposit deposit) {
        this.depositId = deposit.getId();
        this.timestamp = deposit.getDepositTimestamp();
        this.amount = deposit.getAmount();
        this.processedByEmployee = deposit.getProcessedByEmployee();
        this.notes = deposit.getNotes();

        if (deposit.getTransaction() != null) {
            this.transactionId = deposit.getTransaction().getId();
        }
        if (deposit.getToAccount() != null) {
            this.toAccountNumber = deposit.getToAccount().getAccountNumber();
            if (deposit.getToAccount().getOwner() != null) {
                this.toAccountOwnerName = deposit.getToAccount().getOwner().getFirstName() + " " + deposit.getToAccount().getOwner().getLastName();
            }
        }
    }


}