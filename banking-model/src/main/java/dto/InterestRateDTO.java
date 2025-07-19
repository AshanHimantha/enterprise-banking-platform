package dto;

import entity.InterestRate;
import enums.AccountLevel;
import enums.AccountType;
import java.math.BigDecimal;


public class InterestRateDTO {
    private AccountType accountType;
    private AccountLevel accountLevel;
    private BigDecimal annualRate;
    private String description;


    public InterestRateDTO() {}

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public AccountLevel getAccountLevel() {
        return accountLevel;
    }

    public void setAccountLevel(AccountLevel accountLevel) {
        this.accountLevel = accountLevel;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public void setAnnualRate(BigDecimal annualRate) {
        this.annualRate = annualRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InterestRateDTO(InterestRate entity) {
        this.accountType = entity.getId().getAccountType();
        this.accountLevel = entity.getId().getAccountLevel();
        this.annualRate = entity.getAnnualRate();
        this.description = entity.getDescription();
    }


}