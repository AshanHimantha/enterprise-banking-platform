package auth.service;


import dto.RegisterDTO;
import entity.User;
import jakarta.ejb.Local;
import java.util.Optional;

@Local
public interface AuthService {
    // Change the signature to use the DTO
    void registerUser(RegisterDTO registerDTO, String role);
    Optional<String> login(String username, String password);
}