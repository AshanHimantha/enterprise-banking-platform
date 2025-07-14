package service;

import dto.DashboardSummaryDTO;
import dto.TransactionDTO;
import entity.DashboardAccountDTO;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;

@Stateless
public class DashboardServiceImpl implements DashboardService {

    @EJB
    private AccountService accountService;

    @EJB
    private TransactionService transactionService;

    @Override
    public DashboardSummaryDTO getDashboardSummary(String username) {
        // 1. Get the user's accounts
        List<DashboardAccountDTO> userAccounts = accountService.findAccountsByUsername(username);

        // 2. Get the user's most recent transactions
        // For simplicity, we get history from the first account if it exists.
        // A more advanced version would aggregate transactions from ALL accounts.
        List<TransactionDTO> recentTransactions = java.util.Collections.emptyList();
        if (userAccounts != null && !userAccounts.isEmpty()) {
            String primaryAccountNumber = userAccounts.get(0).getAccountNumber();
            // Get the latest 5 transactions (page 1, size 5)
            recentTransactions = transactionService.getTransactionHistory(username, primaryAccountNumber, null, null, null, 1, 5);
        }

        // 3. Assemble the final DTO
        DashboardSummaryDTO summary = new DashboardSummaryDTO();
        summary.setAccounts(userAccounts);
        summary.setRecentTransactions(recentTransactions);

        return summary;
    }
}