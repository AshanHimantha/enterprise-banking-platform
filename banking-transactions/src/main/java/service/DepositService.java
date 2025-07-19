package service;

import dto.DepositHistoryDTO;
import dto.DepositRequestDTO;
import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;

@Local
public interface DepositService {

    void processDeposit(String employeeUsername, DepositRequestDTO request);
    List<DepositHistoryDTO> getDepositHistory(
            String searchTerm, // Search by account number, owner name, or employee name
            LocalDate startDate,
            LocalDate endDate,
            int pageNumber,
            int pageSize
    );

    long countDepositHistory(String searchTerm, LocalDate startDate, LocalDate endDate);
}