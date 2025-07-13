package auth.service;


import dto.EmailVerificationDTO;
import dto.RegisterDTO;
import dto.TokenUpdateDTO;
import jakarta.ejb.Local;
import java.util.Optional;

@Local
public interface AuthService {
    // Change the signature to use the DTO
    void registerUser(RegisterDTO registerDTO);
    Optional<String> login(String usernameOrEmail, String password);
    Optional<String> verifyEmail(EmailVerificationDTO verificationDTO);
    Optional<String> verifyLoginCode(String usernameOrEmail, String verificationCode);
    Optional<String> updateJwtToken(TokenUpdateDTO tokenUpdateDTO);
}