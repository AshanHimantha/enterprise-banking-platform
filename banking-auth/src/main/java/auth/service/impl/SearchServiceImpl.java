package auth.service.impl;


import auth.service.SearchService;
import dto.UserSearchResultDTO;
import entity.Account;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE", "CUSTOMER"})
@Interceptors(LoggingInterceptor.class)
public class SearchServiceImpl implements SearchService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public List<UserSearchResultDTO> searchUsers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) { // Also check for empty string
            return java.util.Collections.emptyList();
        }

        // Using LIKE with a wildcard at the beginning and end can be better for a "contains" search
        String searchPattern = "%" + searchTerm.trim().toLowerCase() + "%";

        String jpql = "SELECT a FROM Account a JOIN FETCH a.owner u " +
                "WHERE LOWER(u.username) LIKE :searchPattern " +
                "OR LOWER(u.email) LIKE :searchPattern " +
                "OR a.accountNumber LIKE :searchPattern";

        TypedQuery<Account> query = em.createQuery(jpql, Account.class);
        query.setParameter("searchPattern", searchPattern);
        query.setMaxResults(5);

        List<Account> results = query.getResultList();

        // Step 2: Manually map the results to your DTO in Java.
        return results.stream()
                .map(account -> {
                    String rawUrl = account.getOwner().getProfilePictureUrl();
                    // *** THIS IS THE NEW LOGIC ***
                    String fullApiUrl = null;
                    if (rawUrl != null && !rawUrl.isEmpty()) {
                        // Extract just the filename from the stored path
                        String filename = rawUrl.substring(rawUrl.lastIndexOf('/') + 1);
                        // Prepend the API path to create a full, callable URL
                        fullApiUrl = "/api/user/profile/avatar/image/" + filename;
                    }

                    // Create the DTO with the newly constructed URL
                    return new UserSearchResultDTO(
                            account.getOwner().getEmail(),
                            account.getOwner().getFirstName(),
                            account.getOwner().getLastName(),
                            account.getOwner().getUsername(),
                            fullApiUrl, // Use the constructed URL
                            account.getAccountNumber(),
                            account.getAccountType()
                    );
                })
                .collect(Collectors.toList());
    }
}