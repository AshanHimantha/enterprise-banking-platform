package auth.service.impl;

import annotation.Audit;
import annotation.Logging;
import auth.service.AuthService;
import auth.util.TokenProvider;
import dto.EmailVerificationDTO;
import dto.RegisterDTO;
import dto.TokenUpdateDTO;
import entity.User;
import entity.UserRole;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import mail.EmailService;
import util.LoggingInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;


@Stateless
@Logging
public class AuthServiceImpl implements AuthService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Inject
    private TokenProvider tokenProvider;

    @EJB
    private EmailService emailService;

    // The method signature is now corrected to use RegisterDTO
    @Override
    public void registerUser(RegisterDTO registerDTO) {
        // 1. Validate input - Basic null checks
        if (registerDTO == null) {
            throw new IllegalArgumentException("Registration data cannot be null.");
        }



        // Validate required fields
        validateRequiredFields(registerDTO);

        // Validate field formats
        validateFieldFormats(registerDTO);

        // Validate business rules
        validateBusinessRules(registerDTO);

        // Check if username is already taken
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class);
        query.setParameter("username", registerDTO.getUsername());

        if (query.getSingleResult() > 0) {
            throw new IllegalArgumentException("This username is already taken.");
        }

        // Check if email is already taken
        TypedQuery<Long> emailQuery = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
        emailQuery.setParameter("email", registerDTO.getEmail());

        if (emailQuery.getSingleResult() > 0) {
            throw new IllegalArgumentException("This email is already registered.");
        }

        // Check if phone number is already taken
        TypedQuery<Long> phoneQuery = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.phoneNumber = :phone", Long.class);
        phoneQuery.setParameter("phone", registerDTO.getPhoneNumber());

        if (phoneQuery.getSingleResult() > 0) {
            throw new IllegalArgumentException("This phone number is already registered.");
        }



        // 2. Map DTO to User entity
        User newUser = new User();
        newUser.setFirstName(registerDTO.getFirstName().trim());
        newUser.setLastName(registerDTO.getLastName().trim());
        newUser.setEmail(registerDTO.getEmail().trim().toLowerCase());
        newUser.setPhoneNumber(registerDTO.getPhoneNumber().trim());
        newUser.setUsername(registerDTO.getUsername().trim());


        // 3. Set server-side defaults
        newUser.setPassword(hashPassword(registerDTO.getPassword()));



        // Your required defaults
        newUser.setEmailVerified(false);
        newUser.setKycStatus(KycStatus.PENDING);
        newUser.setAccountLevel(AccountLevel.BRONZE);
        newUser.setRegisteredDate(LocalDateTime.now());
        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        newUser.setEmailVerificationCode(verificationCode);

        try {
            // 4. Persist the new user and their role
            em.persist(newUser);
            em.flush(); // Ensure user is persisted before creating role

            UserRole userRole = new UserRole();
            userRole.setUsername(newUser.getUsername());
            userRole.setRolename("NONE"); // Assign the NONE role
            em.persist(userRole);


            emailService.sendVerificationEmail(newUser.getEmail(), newUser.getUsername(), verificationCode);
        } catch (Exception e) {
            // Log the error and throw a more user-friendly exception
            System.err.println("Error registering user " + registerDTO.getUsername() + ": " + e.getMessage());
            throw new RuntimeException("Registration failed. Please try again.", e);
        }
    }


    @Override
    public Optional<String> verifyEmail(EmailVerificationDTO verificationDTO) {
        if (verificationDTO == null || verificationDTO.getEmail() == null || verificationDTO.getCode() == null) {
            return Optional.empty();
        }

        // Find the user by their email
        TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", verificationDTO.getEmail().trim().toLowerCase());

        try {
            User user = query.getSingleResult();

            // Check if the user is already verified
            if (user.isEmailVerified()) {
                // Already verified, generate and return JWT token
                String token = tokenProvider.createToken(user);
                System.out.println("Email already verified for user: " + user.getUsername() + ", returning token");
                return Optional.of(token);
            }

            // Check if the provided code matches the one in the database
            if (user.getEmailVerificationCode() != null && user.getEmailVerificationCode().equals(verificationDTO.getCode().trim().toUpperCase())) {

                // Codes match! Update the user.
                user.setEmailVerified(true);
                user.setEmailVerificationCode(null); // Clear the code so it can't be reused
                em.merge(user); // Use merge to update the existing entity

                // Generate and return JWT token for successful email verification
                String token = tokenProvider.createToken(user);
                System.out.println("Email successfully verified for user: " + user.getUsername() + ", returning token");
                return Optional.of(token);
            }
        } catch (Exception e) {
            // User not found
            System.err.println("Email verification failed: User not found for email " + verificationDTO.getEmail());
            return Optional.empty();
        }

        // Codes did not match
        System.err.println("Email verification failed: Invalid code for email " + verificationDTO.getEmail());
        return Optional.empty();
    }

    private void validateRequiredFields(RegisterDTO registerDTO) {
        if (registerDTO.getFirstName() == null || registerDTO.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (registerDTO.getLastName() == null || registerDTO.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        if (registerDTO.getEmail() == null || registerDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (registerDTO.getPhoneNumber() == null || registerDTO.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required.");
        }
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
    }

    private void validateFieldFormats(RegisterDTO registerDTO) {
        // Email format validation
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        if (!registerDTO.getEmail().matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format.");
        }

        // Phone number format validation (assuming international format)
        String phoneRegex = "^\\+?[1-9]\\d{1,14}$";
        String cleanPhone = registerDTO.getPhoneNumber().replaceAll("[\\s()-]", "");
        if (!cleanPhone.matches(phoneRegex)) {
            throw new IllegalArgumentException("Invalid phone number format. Please use international format (+1234567890).");
        }

        // Username format validation
        String usernameRegex = "^[a-zA-Z0-9_]{3,20}$";
        if (!registerDTO.getUsername().matches(usernameRegex)) {
            throw new IllegalArgumentException("Username must be 3-20 characters long and contain only letters, numbers, and underscores.");
        }
    }

    private void validateBusinessRules(RegisterDTO registerDTO) {
        // Name length validation
        if (registerDTO.getFirstName().trim().length() < 2 || registerDTO.getFirstName().trim().length() > 50) {
            throw new IllegalArgumentException("First name must be between 2 and 50 characters.");
        }
        if (registerDTO.getLastName().trim().length() < 2 || registerDTO.getLastName().trim().length() > 50) {
            throw new IllegalArgumentException("Last name must be between 2 and 50 characters.");
        }

        // Name should not contain numbers or special characters
        String nameRegex = "^[a-zA-Z\\s]+$";
        if (!registerDTO.getFirstName().matches(nameRegex)) {
            throw new IllegalArgumentException("First name should only contain letters and spaces.");
        }
        if (!registerDTO.getLastName().matches(nameRegex)) {
            throw new IllegalArgumentException("Last name should only contain letters and spaces.");
        }

        // Email length validation
        if (registerDTO.getEmail().length() > 100) {
            throw new IllegalArgumentException("Email address is too long (maximum 100 characters).");
        }

        // Phone number length validation
        String cleanPhone = registerDTO.getPhoneNumber().replaceAll("[\\s()-]", "");
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            throw new IllegalArgumentException("Phone number must be between 10 and 15 digits.");
        }

        // Password strength validation
        if (!isValidPassword(registerDTO.getPassword())) {
            throw new IllegalArgumentException("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number.");
        }

        // Additional password security checks
        if (registerDTO.getPassword().length() > 128) {
            throw new IllegalArgumentException("Password is too long (maximum 128 characters).");
        }

        // Check for common weak passwords
        String[] weakPasswords = {"password", "123456", "qwerty", "abc123", "password123"};
        String lowercasePassword = registerDTO.getPassword().toLowerCase();
        for (String weak : weakPasswords) {
            if (lowercasePassword.contains(weak)) {
                throw new IllegalArgumentException("Password contains common weak patterns. Please choose a stronger password.");
            }
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasLowercase = !password.equals(password.toUpperCase());
        boolean hasNumber = password.matches(".*\\d.*");
        return hasUppercase && hasLowercase && hasNumber;
    }

    @Override
    public Optional<String> login(String usernameOrEmail, String password) {
        // Validate input
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            return Optional.empty();
        }
        if (password == null || password.isEmpty()) {
            return Optional.empty();
        }

        // Try to find user by username or email (case-insensitive)
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)",
            User.class
        );
        query.setParameter("identifier", usernameOrEmail.trim());

        try {
            User user = query.getSingleResult();

            // Validate the password - BOTH user existence AND password must be correct
            if (!user.getPassword().equals(hashPassword(password))) {
                System.err.println("Invalid credentials for: " + usernameOrEmail);
                return Optional.empty(); // Return same response as user not found
            }

            // Check if user status is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                System.err.println("Login denied for user " + usernameOrEmail + " - User status: " + user.getStatus());
                return Optional.of(user.getStatus().name()); // Return the status name directly
            }

            // Both user exists AND password is correct AND user is active - proceed with verification
            // Generate login verification code
            String loginVerificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            user.setEmailVerificationCode(loginVerificationCode);
            em.merge(user);

            // Send login verification email
            emailService.sendLoginVerificationCode(user.getEmail(), user.getUsername(), loginVerificationCode);

            System.out.println("Login verification code sent to " + user.getEmail());

            // Return success indicator - verification code was sent
            return Optional.of("VERIFICATION_CODE_SENT");

        } catch (NoResultException e) {
            // User not found - return same response as invalid password for security
            System.err.println("Invalid credentials for: " + usernameOrEmail);
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Login failed for user " + usernameOrEmail + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> verifyLoginCode(String usernameOrEmail, String verificationCode) {
        // Validate input
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            return Optional.empty();
        }
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            return Optional.empty();
        }

        // Try to find user by username or email (case-insensitive)
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:identifier) OR LOWER(u.email) = LOWER(:identifier)",
            User.class
        );
        query.setParameter("identifier", usernameOrEmail.trim());

        try {
            User user = query.getSingleResult();

            // Check if the provided code matches the one in the database
            if (user.getEmailVerificationCode() != null &&
                user.getEmailVerificationCode().equals(verificationCode.trim().toUpperCase())) {

                // Clear the verification code so it can't be reused
                user.setEmailVerificationCode(null);

                // If email is not verified, mark it as verified since user proved access to email
                if (!user.isEmailVerified()) {
                    user.setEmailVerified(true);
                    System.out.println("Email automatically verified for user: " + user.getUsername());
                }

                // Update last login date
                user.setLastLoginDate(LocalDateTime.now());

                em.merge(user);

                // Generate and return JWT token for successful login
                String token = tokenProvider.createToken(user);
                System.out.println("Login verification successful for user: " + user.getUsername());
                return Optional.of(token);
            }
        } catch (Exception e) {
            System.err.println("Login verification failed for user " + usernameOrEmail + ": " + e.getMessage());
            return Optional.empty();
        }

        System.err.println("Login verification failed: Invalid code for user " + usernameOrEmail);
        return Optional.empty();
    }

    @Override
    public Optional<String> updateJwtToken(TokenUpdateDTO tokenUpdateDTO) {
        // Validate input
        if (tokenUpdateDTO == null ||
            tokenUpdateDTO.getCurrentToken() == null ||
            tokenUpdateDTO.getCurrentToken().trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            // Validate the current token
            if (!tokenProvider.validateToken(tokenUpdateDTO.getCurrentToken())) {
                System.err.println("Token validation failed: Invalid or expired token");
                return Optional.empty();
            }

            // Extract claims from the current token
            var claims = tokenProvider.getClaimsFromToken(tokenUpdateDTO.getCurrentToken());
            String usernameFromToken = claims.getSubject();

            // If username is provided in DTO, verify it matches the token
            if (tokenUpdateDTO.getUsername() != null &&
                !tokenUpdateDTO.getUsername().trim().isEmpty() &&
                !usernameFromToken.equals(tokenUpdateDTO.getUsername().trim())) {
                System.err.println("Token update failed: Username mismatch");
                return Optional.empty();
            }

            // Find the user in the database
            TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", usernameFromToken);

            User user = query.getSingleResult();

            // Generate new JWT token
            String newToken = tokenProvider.createToken(user);
            System.out.println("JWT token successfully updated for user: " + user.getUsername());

            return Optional.of(newToken);

        } catch (Exception e) {
            System.err.println("Token update failed: " + e.getMessage());
            return Optional.empty();
        }
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