package entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "interest_accrual")
public class InterestAccrual implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The account this interest was calculated for.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // The date for which this interest was calculated.
    @Column(nullable = false)
    private LocalDate accrualDate;

    // The calculated interest amount for that single day.
    @Column(nullable = false, precision = 19, scale = 12)
    private BigDecimal interestAmount;

    // The balance of the account at the time of calculation.
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal closingBalance;

    // The annual rate used for the calculation.
    @Column(nullable = false, precision = 10, scale = 5)
    private BigDecimal annualRateUsed;

    public boolean isPaidOut() {
        return paidOut;
    }

    public void setPaidOut(boolean paidOut) {
        this.paidOut = paidOut;
    }

    public BigDecimal getAnnualRateUsed() {
        return annualRateUsed;
    }

    public void setAnnualRateUsed(BigDecimal annualRateUsed) {
        this.annualRateUsed = annualRateUsed;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public LocalDate getAccrualDate() {
        return accrualDate;
    }

    public void setAccrualDate(LocalDate accrualDate) {
        this.accrualDate = accrualDate;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Flag to show if this accrual has been paid out.
    @Column(nullable = false)
    private boolean paidOut = false;


}