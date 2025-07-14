package dto;

import java.math.BigDecimal;

public class BillPaymentRequestDTO {
    private Long savedBillerId;
    private BigDecimal amount;
    private String userMemo;

    public Long getSavedBillerId() {
        return savedBillerId;
    }

    public void setSavedBillerId(Long savedBillerId) {
        this.savedBillerId = savedBillerId;
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
// Getters and Setters...
}