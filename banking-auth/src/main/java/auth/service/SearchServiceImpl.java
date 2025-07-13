package auth.service;


import dto.UserSearchResultDTO;
import entity.Account;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors; // Import Collectors

@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE, CUSTOMER"})
public class SearchServiceImpl implements SearchService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public List<UserSearchResultDTO> searchUsers(String searchTerm) {
        if (searchTerm == null ) {
            return java.util.Collections.emptyList();
        }

        String searchPattern = searchTerm.trim().toLowerCase() + "%";

        // Step 1: Query for the full Account entities.
        // A JOIN FETCH is used to eagerly load the associated User object
        // in a single database trip, which is very efficient.
        String jpql = "SELECT a FROM Account a JOIN FETCH a.owner u " +
                "WHERE LOWER(u.username) LIKE :searchPattern " +
                "OR LOWER(u.email) LIKE :searchPattern " +
                "OR a.accountNumber LIKE :searchPattern";

        TypedQuery<Account> query = em.createQuery(jpql, Account.class);
        query.setParameter("searchPattern", searchPattern);
        query.setMaxResults(5);

        List<Account> results = query.getResultList();

        // Step 2: Manually map the results to your DTO in Java.
        // This is more reliable than a JPQL constructor expression.
        return results.stream()
                .map(account -> new UserSearchResultDTO(
                        account.getOwner().getEmail(),
                        account.getOwner().getFirstName(),
                        account.getOwner().getLastName(),
                        account.getOwner().getUsername(),
                        account.getOwner().getProfilePictureUrl(),
                        account.getAccountNumber(),
                        account.getAccountType()
                ))
                .collect(Collectors.toList());
    }
}