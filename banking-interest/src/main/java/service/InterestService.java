package service;

import jakarta.ejb.Local;

@Local
public interface InterestService {
    void accrueDailyInterestForAllEligibleAccounts();
    void payoutInterestForAllEligibleAccounts();
}