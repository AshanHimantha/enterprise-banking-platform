package service;

import dto.InterestRateDTO;
import jakarta.ejb.Local;

import java.util.List;

@Local
public interface InterestService {
    void accrueDailyInterestForAllEligibleAccounts();
    void payoutInterestForAllEligibleAccounts();
    List<InterestRateDTO> getAllInterestRates();
    InterestRateDTO saveOrUpdateInterestRate(InterestRateDTO rateDTO);
}