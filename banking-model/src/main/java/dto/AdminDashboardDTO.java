package dto;

import java.math.BigDecimal;

public class AdminDashboardDTO {
    // User statistics
    private long totalUsers;
    private long newUsersToday;
    private long activeUsers;

    // Transaction statistics
    private long totalTransactions;
    private long transactionsToday;
    private BigDecimal totalTransactionVolume; // Sum of all transaction amounts

    // Account statistics
    private long totalAccounts;
    private BigDecimal totalSystemAssets; // Sum of all account balances

    private ChartDataDTO newUsersChart;

    public ChartDataDTO getNewUsersChart() {
        return newUsersChart;
    }

    public void setNewUsersChart(ChartDataDTO newUsersChart) {
        this.newUsersChart = newUsersChart;
    }

    public ChartDataDTO getTransactionVolumeChart() {
        return transactionVolumeChart;
    }

    public void setTransactionVolumeChart(ChartDataDTO transactionVolumeChart) {
        this.transactionVolumeChart = transactionVolumeChart;
    }

    private ChartDataDTO transactionVolumeChart;

    // KYC statistics
    private long pendingKycSubmissions;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getNewUsersToday() {
        return newUsersToday;
    }

    public void setNewUsersToday(long newUsersToday) {
        this.newUsersToday = newUsersToday;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getTransactionsToday() {
        return transactionsToday;
    }

    public void setTransactionsToday(long transactionsToday) {
        this.transactionsToday = transactionsToday;
    }

    public BigDecimal getTotalTransactionVolume() {
        return totalTransactionVolume;
    }

    public void setTotalTransactionVolume(BigDecimal totalTransactionVolume) {
        this.totalTransactionVolume = totalTransactionVolume;
    }

    public long getTotalAccounts() {
        return totalAccounts;
    }

    public void setTotalAccounts(long totalAccounts) {
        this.totalAccounts = totalAccounts;
    }

    public BigDecimal getTotalSystemAssets() {
        return totalSystemAssets;
    }

    public void setTotalSystemAssets(BigDecimal totalSystemAssets) {
        this.totalSystemAssets = totalSystemAssets;
    }

    public long getPendingKycSubmissions() {
        return pendingKycSubmissions;
    }

    public void setPendingKycSubmissions(long pendingKycSubmissions) {
        this.pendingKycSubmissions = pendingKycSubmissions;
    }
}