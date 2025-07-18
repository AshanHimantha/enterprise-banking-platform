package dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A comprehensive DTO to hold all data needed for the user's main dashboard screen.
 */
public class DashboardSummaryDTO {
    // A summary of the user's accounts
    private List<DashboardAccountDTO> accounts;

    private BigDecimal totalBalance;
    private BigDecimal monthlySpending; // Total debits in the last 30 days
    private List<ScheduledPaymentDTO> upcomingPayments;

    // A list of the 5 most recent transactions across all accounts
    private List<TransactionDTO> recentTransactions;

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getMonthlySpending() {
        return monthlySpending;
    }

    public void setMonthlySpending(BigDecimal monthlySpending) {
        this.monthlySpending = monthlySpending;
    }

    public List<ScheduledPaymentDTO> getUpcomingPayments() {
        return upcomingPayments;
    }

    public void setUpcomingPayments(List<ScheduledPaymentDTO> upcomingPayments) {
        this.upcomingPayments = upcomingPayments;
    }

    public List<DashboardAccountDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<DashboardAccountDTO> accounts) {
        this.accounts = accounts;
    }

    public List<TransactionDTO> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionDTO> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}