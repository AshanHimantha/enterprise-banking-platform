package auth.service.impl;



import auth.service.AdminService;
import entity.KycDocument;
import entity.User;
import entity.UserRole;
import enums.AccountType;
import enums.KycStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.AccountService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Stateless
public class AdminServiceImpl implements AdminService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private AccountService accountService;

    @Override
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public void approveKycAndAssignRole(String username, String reviewNotes, String reviewedBy) {
        // Find the user
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult();

        // Find the KYC document
        TypedQuery<KycDocument> kycQuery = em.createQuery("SELECT k FROM KycDocument k WHERE k.user.username = :username", KycDocument.class);
        kycQuery.setParameter("username", username);
        KycDocument kycDocument = kycQuery.getSingleResult();

        // 1. Update the USER entity with review information (moved from KycDocument)
        user.setKycReviewNotes(reviewNotes);
        user.setKycReviewedBy(reviewedBy);
        user.setKycReviewedAt(LocalDateTime.now());

        // 2. Update the user's KYC status
        user.setKycStatus(KycStatus.VERIFIED);
        em.merge(user);

        // 3. Remove the 'NONE' role
        TypedQuery<UserRole> findNoneRoleQuery = em.createQuery(
                "SELECT ur FROM UserRole ur WHERE ur.username = :username AND ur.rolename = 'NONE'", UserRole.class);
        findNoneRoleQuery.setParameter("username", username);

        try {
            UserRole noneRole = findNoneRoleQuery.getSingleResult();
            em.remove(noneRole);
            System.out.println("Removed NONE role from user: " + username);
        } catch (jakarta.persistence.NoResultException e) {
            System.out.println("User " + username + " did not have a NONE role to remove.");
        }

        // 4. Add the 'CUSTOMER' role
        TypedQuery<Long> customerRoleQuery = em.createQuery(
                "SELECT COUNT(ur) FROM UserRole ur WHERE ur.username = :username AND ur.rolename = 'CUSTOMER'", Long.class);
        customerRoleQuery.setParameter("username", username);

        if (customerRoleQuery.getSingleResult() == 0) {
            UserRole customerRole = new UserRole();
            customerRole.setUsername(username);
            customerRole.setRolename("CUSTOMER");
            em.persist(customerRole);
            System.out.println("Assigned CUSTOMER role to user: " + username);
        } else {
            System.out.println("User " + username + " already has the CUSTOMER role.");
        }

        // 5. Create account for the user with initial deposit of 0
        try {
            accountService.createAccountForNewUser(user, BigDecimal.ZERO , AccountType.SAVING);
            System.out.println("Created account for user: " + username);
        } catch (Exception e) {
            System.err.println("Failed to create account for user: " + username + ". Error: " + e.getMessage());
            // Log the error but don't fail the entire KYC approval process
        }
    }

    @Override
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public void rejectKyc(String username, String reviewNotes, String reviewedBy) {
        // Find the user
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult();

        // Find the KYC document
        TypedQuery<KycDocument> kycQuery = em.createQuery("SELECT k FROM KycDocument k WHERE k.user.username = :username", KycDocument.class);
        kycQuery.setParameter("username", username);
        KycDocument kycDocument = kycQuery.getSingleResult();

        // Store review information in USER entity (moved from KycDocument)
        user.setKycReviewNotes(reviewNotes);
        user.setKycReviewedBy(reviewedBy);
        user.setKycReviewedAt(LocalDateTime.now());

        // Delete the uploaded files from file system
        deleteKycFiles(kycDocument);

        // Ensure the entity is managed before removal
        if (!em.contains(kycDocument)) {
            kycDocument = em.merge(kycDocument);
        }

        // Remove the KYC document completely from database
        em.remove(kycDocument);

        // Force immediate flush to ensure deletion
        em.flush();

        // Update the user's KYC status to REJECTED
        user.setKycStatus(KycStatus.REJECTED);
        em.merge(user);

        // Force flush to ensure user status is updated
        em.flush();

        System.out.println("Rejected and completely removed KYC document from database for user: " + username + " with notes: " + reviewNotes);
    }

    private void deleteKycFiles(KycDocument kycDocument) {
        try {
            // Delete front photo file
            if (kycDocument.getIdFrontPhotoPath() != null) {
                File frontFile = new File(kycDocument.getIdFrontPhotoPath());
                if (frontFile.exists()) {
                    boolean deleted = frontFile.delete();
                    System.out.println("Front photo file deleted: " + deleted);
                }
            }

            // Delete back photo file
            if (kycDocument.getIdBackPhotoPath() != null) {
                File backFile = new File(kycDocument.getIdBackPhotoPath());
                if (backFile.exists()) {
                    boolean deleted = backFile.delete();
                    System.out.println("Back photo file deleted: " + deleted);
                }
            }
        } catch (Exception e) {
            System.err.println("Error deleting KYC files: " + e.getMessage());
            // Continue with database deletion even if file deletion fails
        }
    }
}