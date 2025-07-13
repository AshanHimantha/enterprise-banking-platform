package dto;


import entity.User;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;

/**
 * A Data Transfer Object representing a User for administrative purposes.
 * It omits sensitive fields like passwords and verification codes.
 */
public class UserDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private boolean emailVerified;
    private String phoneNumber;
    private KycStatus kycStatus;
    private AccountLevel accountLevel;
    private UserStatus status;
    private String profilePictureUrl;

    // A no-argument constructor is required for some frameworks.
    public UserDTO() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public AccountLevel getAccountLevel() {
        return accountLevel;
    }

    public void setAccountLevel(AccountLevel accountLevel) {
        this.accountLevel = accountLevel;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    // A convenient constructor to map from the database Entity to this DTO.
    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.emailVerified = user.isEmailVerified();
        this.phoneNumber = user.getPhoneNumber();
        this.kycStatus = user.getKycStatus();
        this.accountLevel = user.getAccountLevel();
        this.status = user.getStatus();
        this.profilePictureUrl = user.getProfilePictureUrl();
    }

    // --- Generate Getters and Setters for all fields below ---
    // (You can use your IDE's "Generate" function for this)
}