package service;

import dto.AdminDashboardDTO;
import dto.ChartDataDTO;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@RolesAllowed({"ADMIN", "EMPLOYEE"})
@Interceptors(LoggingInterceptor.class)
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public AdminDashboardDTO getDashboardSummary() {
        AdminDashboardDTO dto = new AdminDashboardDTO();

        // --- User Statistics ---
        dto.setTotalUsers(em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult());
        dto.setNewUsersToday(em.createQuery("SELECT COUNT(u) FROM User u WHERE u.registeredDate >= :today", Long.class)
                .setParameter("today", LocalDate.now().atStartOfDay()).getSingleResult());
        dto.setActiveUsers(em.createQuery("SELECT COUNT(u) FROM User u WHERE u.status = :activeStatus", Long.class)
                .setParameter("activeStatus", UserStatus.ACTIVE).getSingleResult());

        // --- Transaction Statistics ---
        dto.setTotalTransactions(em.createQuery("SELECT COUNT(t) FROM Transaction t", Long.class).getSingleResult());
        dto.setTransactionsToday(em.createQuery("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :today", Long.class)
                .setParameter("today", LocalDate.now().atStartOfDay()).getSingleResult());

        BigDecimal totalVolume = em.createQuery("SELECT SUM(t.amount) FROM Transaction t", BigDecimal.class).getSingleResult();
        dto.setTotalTransactionVolume(totalVolume != null ? totalVolume : BigDecimal.ZERO);

        // --- Account Statistics ---
        dto.setTotalAccounts(em.createQuery("SELECT COUNT(a) FROM Account a", Long.class).getSingleResult());

        BigDecimal totalAssets = em.createQuery("SELECT SUM(a.balance) FROM Account a WHERE a.owner IS NOT NULL", BigDecimal.class).getSingleResult();
        dto.setTotalSystemAssets(totalAssets != null ? totalAssets : BigDecimal.ZERO);

        // --- KYC Statistics ---
        dto.setPendingKycSubmissions(em.createQuery("SELECT COUNT(u) FROM User u WHERE u.kycStatus = :pendingStatus", Long.class)
                .setParameter("pendingStatus", KycStatus.PENDING).getSingleResult());

        // --- Chart Data ---
        dto.setNewUsersChart(getNewUsersForLast7Days());
        dto.setTransactionVolumeChart(getTransactionVolumeForLast7Days());

        return dto;
    }



    private ChartDataDTO getNewUsersForLast7Days() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);

        // *** THE FIX: Query for raw data, not a new DTO ***
        // The query now returns a List of Object arrays (List<Object[]>)
        String jpql = "SELECT FUNCTION('DATE', u.registeredDate) as regDate, COUNT(u.id) FROM User u " +
                "WHERE u.registeredDate >= :startDate " +
                "GROUP BY regDate " +
                "ORDER BY regDate ASC";

        List<Object[]> results = em.createQuery(jpql, Object[].class)
                .setParameter("startDate", sevenDaysAgo.atStartOfDay())
                .getResultList();

        return buildChartDataFromResults(results, sevenDaysAgo);
    }

    private ChartDataDTO getTransactionVolumeForLast7Days() {
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);

        // *** APPLY THE SAME FIX HERE ***
        // Query for a List of Object arrays.
        String jpql = "SELECT FUNCTION('DATE', t.transactionDate) as txDate, SUM(t.amount) FROM Transaction t " +
                "WHERE t.transactionDate >= :startDate " +
                "GROUP BY txDate " +
                "ORDER BY txDate ASC";

        List<Object[]> results = em.createQuery(jpql, Object[].class)
                .setParameter("startDate", sevenDaysAgo.atStartOfDay())
                .getResultList();

        return buildChartDataFromResults(results, sevenDaysAgo);
    }

    // The helper method to build the chart is already designed for this and is correct.
    private ChartDataDTO buildChartDataFromResults(List<Object[]> results, LocalDate startDate) {
        Map<LocalDate, Number> dataMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((java.sql.Date) row[0]).toLocalDate(),
                        row -> (Number) row[1]
                ));

        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM d");

        // Loop through the last 7 days to build the final, complete lists.
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            labels.add(day.format(labelFormatter));
            // If a day had no activity, dataMap.get() will return null.
            // We use getOrDefault to provide 0 in that case.
            data.add(dataMap.getOrDefault(day, 0));
        }

        return new ChartDataDTO(labels, data);
    }
}