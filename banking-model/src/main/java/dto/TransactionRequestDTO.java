package dto;



import java.math.BigDecimal;

/**
 * DTO for initiating a new fund transfer.
 * This represents the JSON body the client will send.
 */
public class TransactionRequestDTO {
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String userMemo; // Optional note from the user

    // --- Getters and Setters ---

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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUserMemo() {
        return userMemo;
    }

    public void setUserMemo(String userMemo) {
        this.userMemo = userMemo;
    }
}