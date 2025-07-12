package auth.service;



import entity.User;
import entity.UserRole;
import enums.KycStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class AdminServiceImpl implements AdminService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public void approveKycAndAssignRole(String username) {
        // Find the user
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult();

        // 1. Update the user's KYC status
        user.setKycStatus(KycStatus.VERIFIED);
        em.merge(user);

        // *** 2. REMOVE THE 'NONE' ROLE ***
        TypedQuery<UserRole> findNoneRoleQuery = em.createQuery(
                "SELECT ur FROM UserRole ur WHERE ur.username = :username AND ur.rolename = 'NONE'", UserRole.class);
        findNoneRoleQuery.setParameter("username", username);

        try {
            UserRole noneRole = findNoneRoleQuery.getSingleResult();
            em.remove(noneRole); // Delete the NONE role from the database
            System.out.println("Removed NONE role from user: " + username);
        } catch (jakarta.persistence.NoResultException e) {
            // This is okay, means the NONE role was already removed.
            System.out.println("User " + username + " did not have a NONE role to remove.");
        }

        // *** 3. ADD THE 'CUSTOMER' ROLE ***
        // Check if the CUSTOMER role is already assigned to prevent duplicates
        TypedQuery<Long> customerRoleQuery = em.createQuery(
                "SELECT COUNT(ur) FROM UserRole ur WHERE ur.username = :username AND ur.rolename = 'CUSTOMER'", Long.class);
        customerRoleQuery.setParameter("username", username);

        if (customerRoleQuery.getSingleResult() == 0) {
            // Assign the CUSTOMER role
            UserRole customerRole = new UserRole();
            customerRole.setUsername(username);
            customerRole.setRolename("CUSTOMER");
            em.persist(customerRole);
            System.out.println("Assigned CUSTOMER role to user: " + username);
        } else {
            System.out.println("User " + username + " already has the CUSTOMER role.");
        }
    }
}