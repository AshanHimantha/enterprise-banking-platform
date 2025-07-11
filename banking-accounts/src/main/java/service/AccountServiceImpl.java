package service;

import entity.Account;
import entity.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;

@Stateless
public class AccountServiceImpl implements AccountService { // Implements the interface

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override // Add the @Override annotation
    public void createAccountForNewUser(User user, BigDecimal initialDeposit) {
        em.persist(user);

        Account account = new Account();
        account.setOwner(user);
        account.setBalance(initialDeposit);
        account.setAccountNumber(UUID.randomUUID().toString());

        em.persist(account);

        System.out.println("Successfully created user: " + user.getUsername() + " and account: " + account.getAccountNumber());
    }
}