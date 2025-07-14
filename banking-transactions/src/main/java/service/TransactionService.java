package service;


import dto.TransactionDTO;
import dto.TransactionRequestDTO;
import enums.TransactionType;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Local
public interface TransactionService {


    void performTransfer(String username, TransactionRequestDTO transactionRequest);

    List<TransactionDTO> getTransactionHistory(
            String username,
            String accountNumber,
            LocalDate startDate,
            LocalDate endDate,
            TransactionType transactionType,
            int pageNumber,
            int pageSize
    );
    void performSystemTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String memo);
}