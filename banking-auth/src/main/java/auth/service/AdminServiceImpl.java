package auth.service;



import entity.KycDocument;
import entity.User;
import entity.UserRole;
import enums.KycStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.AccountService;

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

        // 1. Update the KYC document with review information
        kycDocument.setReviewNotes(reviewNotes);
        kycDocument.setReviewedBy(reviewedBy);
        kycDocument.setReviewedAt(LocalDateTime.now());
        em.merge(kycDocument);

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
            accountService.createAccountForNewUser(user, BigDecimal.ZERO);
            System.out.println("Created account for user: " + username);
        } catch (Exception e) {
            System.err.println("Failed to create account for user: " + username + ". Error: " + e.getMessage());
            // Log the error but don't fail the entire KYC approval process
        }
    }
}