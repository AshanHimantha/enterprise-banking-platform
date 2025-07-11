package auth.service;


import entity.User;
import jakarta.ejb.Local;
import java.util.Optional;

@Local
public interface AuthService {

    /**
     * Registers a new user in the system.
     * Implementations should handle password hashing.
     */
    void registerUser(User user, String role);

    /**
     * Attempts to log in a user with the given credentials.
     *
     * @return An Optional containing the JWT if login is successful, otherwise an empty Optional.
     */
    Optional<String> login(String username, String password);
}