package auth.service.impl;



import auth.service.UserAuditService;
import dto.AdminTransactionDTO;
import dto.DashboardAccountDTO;
import dto.UserAuditDTO;
import dto.UserDTO;
import entity.Account;
import entity.Biller;
import entity.Transaction;
import entity.User;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE"})
@Interceptors(LoggingInterceptor.class)
public class UserAuditServiceImpl implements UserAuditService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public UserAuditDTO getFullUserAudit(String username, int pageNumber, int pageSize) {
        // 1. Fetch the core User entity
        User user = findUserByUsername(username);
        // Map the user entity to its DTO representation
        UserDTO userDetails = new UserDTO(user);

        // 2. Fetch all accounts owned by this User
        List<Account> accounts = findAccountsByUser(user);
        // Map the account entities to their DTO representations
        List<DashboardAccountDTO> accountDTOs = accounts.stream()
                .map(DashboardAccountDTO::new)
                .collect(Collectors.toList());

        // 3. Fetch a paginated list of all transactions involving any of the user's accounts
        List<Transaction> transactions = findTransactionsForAccounts(accounts, pageNumber, pageSize);
        // Map the transaction entities to the detailed admin DTO
        List<AdminTransactionDTO> transactionDTOs = mapTransactionsToAdminDTO(transactions);

        // 4. Assemble the final, comprehensive DTO
        UserAuditDTO auditDTO = new UserAuditDTO();
        auditDTO.setUserDetails(userDetails);
        auditDTO.setAccounts(accountDTOs);
        auditDTO.setTransactions(transactionDTOs);

        return auditDTO;
    }

    // --- Helper Methods ---

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User '" + username + "' not found.");
        }
    }

    private List<Account> findAccountsByUser(User user) {
        return em.createQuery("SELECT a FROM Account a WHERE a.owner = :user ORDER BY a.accountType", Account.class)
                .setParameter("user", user).getResultList();
    }

    private List<Transaction> findTransactionsForAccounts(List<Account> accounts, int pageNumber, int pageSize) {
        if (accounts.isEmpty()) {
            return Collections.emptyList();
        }
        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t WHERE t.fromAccount IN :accounts OR t.toAccount IN :accounts ORDER BY t.transactionDate DESC", Transaction.class);
        query.setParameter("accounts", accounts);

        // Apply pagination
        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }

    /**
     * Maps a list of Transaction entities to a list of AdminTransactionDTOs,
     * intelligently resolving the recipient's name for bill payments.
     */
    private List<AdminTransactionDTO> mapTransactionsToAdminDTO(List<Transaction> transactions) {
        List<AdminTransactionDTO> dtos = new ArrayList<>();
        for (Transaction tx : transactions) {
            AdminTransactionDTO dto = new AdminTransactionDTO(tx); // Basic mapping

            // Special handling for bill payments to show the Biller's name
            if (tx.getTransactionType() == TransactionType.BILL_PAYMENT && tx.getToAccount() != null) {
                try {
                    // Find the biller associated with the internal destination account
                    TypedQuery<Biller> query = em.createQuery("SELECT b FROM Biller b WHERE b.internalAccount = :account", Biller.class);
                    query.setParameter("account", tx.getToAccount());
                    Biller biller = query.getSingleResult();
                    dto.setToOwnerUsername(biller.getBillerName() + " (Biller)");
                } catch (NoResultException e) {
                    dto.setToOwnerUsername("Unknown Biller");
                }
            }
            dtos.add(dto);
        }
        return dtos;
    }
}