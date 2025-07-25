package auth.service.impl;


import annotation.Audit;
import annotation.Logging;
import util.AuditingInterceptor;
import auth.service.UserManagementService;
import dto.EmployeeCreateDTO;
import dto.EmployeeDTO;
import dto.UserDTO;
import entity.User;
import entity.UserRole;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import mail.EmailService;
import util.LoggingInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Audit
@Logging
@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE"})
@Interceptors({LoggingInterceptor.class, AuditingInterceptor.class})
public class UserManagementServiceImpl implements UserManagementService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private EmailService emailService;

    @Override
    public List<UserDTO> getAllUsers() {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class);
        return query.getResultStream()
                .map(UserDTO::new) // Creates a UserDTO from each User entity
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UserDTO> findUserByUsername(String username) {
        return findUserEntityByUsername(username).map(UserDTO::new);
    }

    @Override
    public void suspendUser(String usernameToSuspend, String adminUsername, String reason) {
        User user = findUserEntityByUsername(usernameToSuspend)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new IllegalStateException("User is already suspended.");
        }

        user.setStatus(UserStatus.SUSPENDED);
        user.setKycReviewNotes("Account SUSPENDED by " + adminUsername + ". Reason: " + reason);
        user.setKycReviewedBy(adminUsername);
        user.setKycReviewedAt(LocalDateTime.now());
        em.merge(user);

        try {
            emailService.sendAccountSuspensionEmail(user.getEmail(), user.getUsername(), reason, adminUsername);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send suspension email to " + user.getEmail() + ": " + e.getMessage());

        }
    }

    @Override
    public void reactivateUser(String usernameToReactivate, String adminUsername) {
        User user = findUserEntityByUsername(usernameToReactivate)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalStateException("User account is not currently suspended.");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setKycReviewNotes("Account REACTIVATED by " + adminUsername + ".");
        user.setKycReviewedBy(adminUsername);
        user.setKycReviewedAt(LocalDateTime.now());
        em.merge(user);


        try {
            emailService.sendAccountReactivationEmail(user.getEmail(), user.getUsername(), adminUsername);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send reactivation email to " + user.getEmail() + ": " + e.getMessage());

        }
    }

    @Override
    public List<UserDTO> searchUsers(int page, int limit, AccountLevel accountLevel,
                                   UserStatus status, KycStatus kycStatus,
                                   String username, String email) {

        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;


        if (accountLevel != null) {
            jpql.append(" AND u.accountLevel = ?").append(paramIndex++);
            parameters.add(accountLevel);
        }

        if (status != null) {
            jpql.append(" AND u.status = ?").append(paramIndex++);
            parameters.add(status);
        }

        if (kycStatus != null) {
            jpql.append(" AND u.kycStatus = ?").append(paramIndex++);
            parameters.add(kycStatus);
        }

        if (username != null && !username.trim().isEmpty()) {
            jpql.append(" AND LOWER(u.username) LIKE LOWER(?").append(paramIndex++).append(")");
            parameters.add("%" + username.trim() + "%");
        }

        if (email != null && !email.trim().isEmpty()) {
            jpql.append(" AND LOWER(u.email) LIKE LOWER(?").append(paramIndex).append(")");
            parameters.add("%" + email.trim() + "%");
        }

        jpql.append(" ORDER BY u.id");

        TypedQuery<User> query = em.createQuery(jpql.toString(), User.class);

        // Set parameters
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        // Set pagination
        query.setFirstResult((page - 1) * limit);
        query.setMaxResults(limit);

        return query.getResultStream()
                .map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public int countUsers(AccountLevel accountLevel, UserStatus status, KycStatus kycStatus,
                         String username, String email) {

        StringBuilder jpql = new StringBuilder("SELECT COUNT(u) FROM User u WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        // Build dynamic query with same filters as search
        if (accountLevel != null) {
            jpql.append(" AND u.accountLevel = ?").append(paramIndex++);
            parameters.add(accountLevel);
        }

        if (status != null) {
            jpql.append(" AND u.status = ?").append(paramIndex++);
            parameters.add(status);
        }

        if (kycStatus != null) {
            jpql.append(" AND u.kycStatus = ?").append(paramIndex++);
            parameters.add(kycStatus);
        }

        if (username != null && !username.trim().isEmpty()) {
            jpql.append(" AND LOWER(u.username) LIKE LOWER(?").append(paramIndex++).append(")");
            parameters.add("%" + username.trim() + "%");
        }

        if (email != null && !email.trim().isEmpty()) {
            jpql.append(" AND LOWER(u.email) LIKE LOWER(?").append(paramIndex).append(")");
            parameters.add("%" + email.trim() + "%");
        }

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);


        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getSingleResult().intValue();
    }


    private Optional<User> findUserEntityByUsername(String username) {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public EmployeeDTO createEmployee(EmployeeCreateDTO dto) {
        // --- 1. Validation ---
        validateEmployeeCreateDTO(dto);

        // Check for uniqueness
        if (isUsernameTaken(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
        }
        if (isEmailTaken(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered.");
        }
        if (isPhoneNumberTaken(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number '" + dto.getPhoneNumber() + "' is already registered.");
        }

        User newUser = new User();
        newUser.setFirstName(dto.getFirstName());
        newUser.setLastName(dto.getLastName());
        newUser.setEmail(dto.getEmail());
        newUser.setPhoneNumber(dto.getPhoneNumber());
        newUser.setUsername(dto.getUsername());

        newUser.setPassword(hashPassword(dto.getInitialPassword()));
        newUser.setPasscode(null);

        newUser.setEmailVerified(true);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setRegisteredDate(LocalDateTime.now());

        newUser.setKycStatus(KycStatus.VERIFIED);
        newUser.setAccountLevel(AccountLevel.BRONZE);

        em.persist(newUser);
        em.flush();

        UserRole userRole = new UserRole();
        userRole.setUsername(newUser.getUsername());
        userRole.setRolename(dto.getRole());
        em.persist(userRole);


        return new EmployeeDTO(newUser, List.of(dto.getRole()));
    }

    @Override
    public List<EmployeeDTO> getAllEmployees() {

        TypedQuery<User> query = em.createQuery(
                "SELECT u FROM User u JOIN UserRole ur ON u.username = ur.username WHERE ur.rolename IN ('ADMIN', 'EMPLOYEE')", User.class);

        List<User> employees = query.getResultList();


        return employees.stream()
                .map(user -> new EmployeeDTO(user, findRolesForUser(user.getUsername())))
                .collect(Collectors.toList());
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public EmployeeDTO addRoleToUser(String username, String roleToAdd) {
        validateRole(roleToAdd); // Helper to check if role is "EMPLOYEE" or "ADMIN"
        User user = findUserEntityByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));


        List<String> currentRoles = findRolesForUser(username);
        if (currentRoles.contains(roleToAdd)) {

            return new EmployeeDTO(user, currentRoles);
        }


        UserRole newUserRole = new UserRole();
        newUserRole.setUsername(username);
        newUserRole.setRolename(roleToAdd);
        em.persist(newUserRole);

        currentRoles.add(roleToAdd);
        return new EmployeeDTO(user, currentRoles);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public EmployeeDTO removeRoleFromUser(String username, String roleToRemove) {
        validateRole(roleToRemove);
        User user = findUserEntityByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (findRolesForUser(username).size() <= 1 && roleToRemove.equals("ADMIN")) {

        }


        int deletedCount = em.createQuery("DELETE FROM UserRole ur WHERE ur.username = :username AND ur.rolename = :role")
                .setParameter("username", username)
                .setParameter("role", roleToRemove)
                .executeUpdate();

        if (deletedCount == 0) {

        }

        return new EmployeeDTO(user, findRolesForUser(username)); // Return the new list of roles
    }

    private void validateRole(String role) {
        if (!role.equals("EMPLOYEE") && !role.equals("ADMIN")) {
            throw new IllegalArgumentException("Invalid role. Must be EMPLOYEE or ADMIN.");
        }
    }

    private List<String> findRolesForUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {

            TypedQuery<String> query = em.createQuery(
                    "SELECT ur.rolename FROM UserRole ur WHERE ur.username = :username", String.class);

            query.setParameter("username", username);


            return query.getResultList();

        } catch (Exception e) {

            System.err.println("Error finding roles for user " + username + ": " + e.getMessage());
            return Collections.emptyList();
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
    private void validateEmployeeCreateDTO(EmployeeCreateDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Request data cannot be null.");
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) throw new IllegalArgumentException("Email is required.");
        if (dto.getInitialPassword() == null || dto.getInitialPassword().isEmpty()) throw new IllegalArgumentException("Initial password is required.");
        if (dto.getRole() == null || (!dto.getRole().equals("EMPLOYEE") && !dto.getRole().equals("ADMIN"))) {
            throw new IllegalArgumentException("Invalid role. Must be EMPLOYEE or ADMIN.");
        }

    }

    private boolean isUsernameTaken(String username) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult() > 0;
    }

    private boolean isEmailTaken(String email) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult() > 0;
    }

    private boolean isPhoneNumberTaken(String phone) {
        return em.createQuery("SELECT COUNT(u) FROM User u WHERE u.phoneNumber = :phone", Long.class)
                .setParameter("phone", phone)
                .getSingleResult() > 0;
    }




}