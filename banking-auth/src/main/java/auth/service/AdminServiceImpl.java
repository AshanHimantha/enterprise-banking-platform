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
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Only authorized staff can do this
    public void approveKycAndAssignRole(String username) {
        // Find the user
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult(); // Throws exception if not found, which is good

        // 1. Update the user's KYC status
        user.setKycStatus(KycStatus.VERIFIED);
        em.merge(user);

        // 2. Check if the CUSTOMER role is already assigned to prevent duplicates
        TypedQuery<Long> roleQuery = em.createQuery("SELECT COUNT(ur) FROM UserRole ur WHERE ur.username = :username AND ur.rolename = 'CUSTOMER'", Long.class);
        roleQuery.setParameter("username", username);

        if (roleQuery.getSingleResult() == 0) {
            // 3. Assign the CUSTOMER role
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