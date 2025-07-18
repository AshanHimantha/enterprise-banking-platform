package dto;


public class RevealRequestDTO {

    private String currentPassword;


    public RevealRequestDTO() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}