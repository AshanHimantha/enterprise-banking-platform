package dto;

import enums.AccountType;

/**
 * DTO for a user's request to create a new SAVING or CURRENT account.
 */
public class CreateAccountDTO {
    // The type of account to create.
    private AccountType accountType;



    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }


}