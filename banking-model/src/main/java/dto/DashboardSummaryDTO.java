package dto;

import java.util.List;

/**
 * A comprehensive DTO to hold all data needed for the user's main dashboard screen.
 */
public class DashboardSummaryDTO {
    // A summary of the user's accounts
    private List<DashboardAccountDTO> accounts;

    // A list of the 5 most recent transactions across all accounts
    private List<TransactionDTO> recentTransactions;

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