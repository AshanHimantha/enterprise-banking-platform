package dto;

import java.util.List;

public class UserAuditDTO {
    private UserDTO userDetails; // We can reuse the existing UserDTO
    private List<DashboardAccountDTO> accounts; // Reuse the DashboardAccountDTO
    private List<AdminTransactionDTO> transactions; // Reuse the AdminTransactionDTO for full details

    public UserDTO getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserDTO userDetails) {
        this.userDetails = userDetails;
    }

    public List<DashboardAccountDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<DashboardAccountDTO> accounts) {
        this.accounts = accounts;
    }

    public List<AdminTransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<AdminTransactionDTO> transactions) {
        this.transactions = transactions;
    }
}