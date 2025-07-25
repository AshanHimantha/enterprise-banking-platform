package service.impl;

import annotation.Audit;
import annotation.Logging;
import dto.AdminTransactionDTO;
import entity.Biller;
import entity.Transaction;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import service.TransactionMonitoringService;
import util.LoggingInterceptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Audit
@Logging
@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE"})
@Interceptors(LoggingInterceptor.class)
public class TransactionMonitoringServiceImpl implements TransactionMonitoringService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public List<AdminTransactionDTO> searchTransactions(
            String searchTerm, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate,
            int pageNumber, int pageSize) {

        // Build the dynamic query for fetching the transaction entities
        StringBuilder jpql = new StringBuilder("SELECT t FROM Transaction t WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        buildWhereClause(jpql, parameters, searchTerm, transactionType, startDate, endDate);

        jpql.append(" ORDER BY t.transactionDate DESC");

        TypedQuery<Transaction> query = em.createQuery(jpql.toString(), Transaction.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        // Apply pagination
        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);

        List<Transaction> transactions = query.getResultList();

        // Manually map to DTOs to handle the Biller name lookup
        return mapTransactionsToAdminDTO(transactions);
    }

    @Override
    public long countTransactions(
            String searchTerm, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {

        // Build a similar dynamic query but for counting the results
        StringBuilder jpql = new StringBuilder("SELECT COUNT(t) FROM Transaction t WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        buildWhereClause(jpql, parameters, searchTerm, transactionType, startDate, endDate);

        TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        return query.getSingleResult();
    }


    private void buildWhereClause(
            StringBuilder jpql, Map<String, Object> parameters,
            String searchTerm, TransactionType transactionType,
            LocalDate startDate, LocalDate endDate) {

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            jpql.append(" AND (LOWER(t.fromAccount.accountNumber) LIKE :term OR " +
                    "LOWER(t.toAccount.accountNumber) LIKE :term OR " +
                    "LOWER(t.fromAccount.owner.username) LIKE :term OR " +
                    "LOWER(t.toAccount.owner.username) LIKE :term OR " +
                    "LOWER(t.description) LIKE :term)");
            parameters.put("term", "%" + searchTerm.toLowerCase().trim() + "%");
        }
        if (transactionType != null) {
            jpql.append(" AND t.transactionType = :type");
            parameters.put("type", transactionType);
        }
        if (startDate != null) {
            jpql.append(" AND t.transactionDate >= :startDate");
            parameters.put("startDate", startDate.atStartOfDay());
        }
        if (endDate != null) {
            jpql.append(" AND t.transactionDate < :endDate");
            // Use "<" with the start of the next day to include the entire end date
            parameters.put("endDate", endDate.plusDays(1).atStartOfDay());
        }
    }


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