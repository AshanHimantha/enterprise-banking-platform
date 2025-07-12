package auth.service;


import dto.EmailVerificationDTO;
import dto.RegisterDTO;
import jakarta.ejb.Local;
import java.util.Optional;

@Local
public interface AuthService {
    // Change the signature to use the DTO
    void registerUser(RegisterDTO registerDTO, String role);
    Optional<String> login(String usernameOrEmail, String password);
    boolean verifyEmail(EmailVerificationDTO verificationDTO);

}