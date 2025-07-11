package auth.service;


import auth.util.TokenProvider;
import entity.User;
import entity.UserRole;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Stateless
public class AuthServiceImpl implements AuthService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject
    private TokenProvider tokenProvider;

    @Override
    public void registerUser(User user, String role) {
        // Hash password and passcode before saving
        user.setPassword(hashPassword(user.getPassword()));
        user.setPasscode(hashPassword(user.getPasscode()));

        // Save the user entity
        em.persist(user);

        // Create and save the user's role
        UserRole userRole = new UserRole();
        userRole.setUsername(user.getUsername());
        userRole.setRolename(role); // Assign the role (e.g., "CUSTOMER")
        em.persist(userRole);
    }

    @Override
    public Optional<String> login(String username, String password) {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);

        try {
            User user = query.getSingleResult();
            // Check if the provided password, when hashed, matches the stored hash
            if (user.getPassword().equals(hashPassword(password))) {
                // Passwords match, generate and return a token
                return Optional.of(tokenProvider.createToken(user));
            }
        } catch (Exception e) {
            // User not found or other error
            System.err.println("Login failed for user " + username + ": " + e.getMessage());
            return Optional.empty();
        }

        return Optional.empty(); // Password did not match
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}