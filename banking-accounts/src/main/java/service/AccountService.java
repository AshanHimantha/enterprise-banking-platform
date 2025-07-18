package service;


import dto.CreateAccountDTO;
import dto.DashboardAccountDTO;
import entity.Account;
import entity.User;
import enums.AccountType;
import jakarta.ejb.Local; // Use @Local for access within the same application
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Local
public interface AccountService {
    void createAccountForNewUser(User user, BigDecimal initialDeposit , AccountType accountType);
    String generateHumanReadableAccountNumber();
    List<DashboardAccountDTO> findAccountsByUsername(String username);
    Optional<DashboardAccountDTO> findAccountByNumberForUser(String accountNumber, String username);
    void verifyAccountOwnership(String username, String accountNumber);
    Account findAccountByNumber(String accountNumber);
    DashboardAccountDTO createNewAccountForUser(String username, CreateAccountDTO dto);
}