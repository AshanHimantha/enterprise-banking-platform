package dto;


import enums.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;


public class ScheduleRequestDTO {


    private String fromAccountNumber;
    private BigDecimal amount;
    private PaymentFrequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private String userMemo;
    private String toAccountNumber;
    private Long billerId;
    private String billerReferenceNumber;


    public ScheduleRequestDTO() {
    }


    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
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

    public String getUserMemo() {
        return userMemo;
    }

    public void setUserMemo(String userMemo) {
        this.userMemo = userMemo;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public Long getBillerId() {
        return billerId;
    }

    public void setBillerId(Long billerId) {
        this.billerId = billerId;
    }

    public String getBillerReferenceNumber() {
        return billerReferenceNumber;
    }

    public void setBillerReferenceNumber(String billerReferenceNumber) {
        this.billerReferenceNumber = billerReferenceNumber;
    }
}