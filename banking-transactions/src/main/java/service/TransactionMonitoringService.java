package service;

import dto.AdminTransactionDTO;
import enums.TransactionType;
import jakarta.ejb.Local;

import java.time.LocalDate;
import java.util.List;

@Local
public interface TransactionMonitoringService {

    List<AdminTransactionDTO> searchTransactions(
            String searchTerm,
            TransactionType transactionType,
            LocalDate startDate,
            LocalDate endDate,
            int pageNumber,
            int pageSize
    );


    long countTransactions(
            String searchTerm,
            TransactionType transactionType,
            LocalDate startDate,
            LocalDate endDate
    );
}