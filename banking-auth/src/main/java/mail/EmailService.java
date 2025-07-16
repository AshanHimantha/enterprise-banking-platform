package mail;

import jakarta.ejb.Local;

@Local
public interface EmailService {
    void sendVerificationEmail(String recipientEmail, String username, String verificationCode);
    void sendLoginVerificationCode(String recipientEmail, String username, String verificationCode);
    void sendAccountSuspensionEmail(String recipientEmail, String username, String reason, String adminUsername);
    void sendAccountReactivationEmail(String recipientEmail, String username, String adminUsername);
    // Add this new method
    void sendEmailWithAttachment(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName, String attachmentType);
}
