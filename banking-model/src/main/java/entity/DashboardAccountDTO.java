package entity;
import enums.AccountType;
import java.math.BigDecimal;

/**
 * DTO for displaying a summary of a user's account on their dashboard.
 */
public class DashboardAccountDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountType accountType;
    private String ownerName; // Helpful to show "John Doe"

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public DashboardAccountDTO(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.balance = account.getBalance();
        this.accountType = account.getAccountType();
        if (account.getOwner() != null) {
            this.ownerName = account.getOwner().getFirstName() + " " + account.getOwner().getLastName();
        }
    }


}