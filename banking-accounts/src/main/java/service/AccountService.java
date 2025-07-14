package service;


import entity.User;
import enums.AccountType;
import jakarta.ejb.Local; // Use @Local for access within the same application
import java.math.BigDecimal;

@Local
public interface AccountService {
    void createAccountForNewUser(User user, BigDecimal initialDeposit , AccountType accountType);
    String generateHumanReadableAccountNumber();
}