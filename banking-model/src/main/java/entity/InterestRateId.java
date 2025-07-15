package entity;


import enums.AccountLevel;
import enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the composite primary key for the InterestRate entity.
 * It's a combination of the AccountType and AccountLevel.
 *
 * An @Embeddable class requires a no-arg constructor, Serializable, and
 * correctly implemented equals() and hashCode() methods.
 */
@Embeddable
public class InterestRateId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_level")
    private AccountLevel accountLevel;

    public InterestRateId() {
    }

    public InterestRateId(AccountType accountType, AccountLevel accountLevel) {
        this.accountType = accountType;
        this.accountLevel = accountLevel;
    }

    // --- Getters and Setters ---

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

    // --- equals() and hashCode() ---
    // These are CRITICAL for using this class as a key in a Map.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InterestRateId that = (InterestRateId) o;
        return accountType == that.accountType && accountLevel == that.accountLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountType, accountLevel);
    }
}