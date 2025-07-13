package dto;


import enums.AccountType;

public class UserSearchResultDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String username;
    private String profilePictureUrl;
    private String accountNumber;
    private AccountType accountType;

    // A no-argument constructor is required.
    public UserSearchResultDTO() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    // A constructor for easy mapping.
    public UserSearchResultDTO(String email, String firstName, String lastName, String username, String profilePictureUrl, String accountNumber, AccountType accountType) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
    }

    // --- Generate Getters and Setters for all fields below ---
}