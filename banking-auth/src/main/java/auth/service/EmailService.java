package auth.service;

import jakarta.ejb.Local;

@Local
public interface EmailService {
    void sendVerificationEmail(String recipientEmail, String username, String verificationCode);
}
