package auth.service;


import dto.UserDTO;
import entity.User;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import mail.EmailService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE"}) // Secures all methods in this EJB
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

        // Send suspension email notification
        try {
            emailService.sendAccountSuspensionEmail(user.getEmail(), user.getUsername(), reason, adminUsername);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send suspension email to " + user.getEmail() + ": " + e.getMessage());
            // Don't throw exception here as the suspension was successful, just email failed
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

        // Send reactivation email notification
        try {
            emailService.sendAccountReactivationEmail(user.getEmail(), user.getUsername(), adminUsername);
        } catch (Exception e) {
            System.err.println("Warning: Failed to send reactivation email to " + user.getEmail() + ": " + e.getMessage());
            // Don't throw exception here as the reactivation was successful, just email failed
        }
    }

    @Override
    public List<UserDTO> searchUsers(int page, int limit, AccountLevel accountLevel,
                                   UserStatus status, KycStatus kycStatus,
                                   String username, String email) {

        StringBuilder jpql = new StringBuilder("SELECT u FROM User u WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        int paramIndex = 1;

        // Build dynamic query with filters
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

        // Set parameters
        for (int i = 0; i < parameters.size(); i++) {
            query.setParameter(i + 1, parameters.get(i));
        }

        return query.getSingleResult().intValue();
    }

    // Helper method to find the raw User entity
    private Optional<User> findUserEntityByUsername(String username) {
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);
        try {
            return Optional.of(query.getSingleResult());
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }


}