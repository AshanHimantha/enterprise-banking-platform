package dto;

public class PinChangeDTO {
    private String currentPassword;
    private String newPin;

    public String getCurrentPassword() {
        return currentPassword;
    }
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
    public String getNewPin() {
        return newPin;
    }
    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }
}
