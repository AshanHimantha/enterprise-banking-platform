package service;


import dto.BillPaymentRequestDTO;
import dto.TransactionDTO;
import dto.TransactionDetailDTO;
import dto.TransactionRequestDTO;
import entity.Account;
import entity.Biller;
import enums.TransactionType;
import jakarta.ejb.Local;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    void payBill(String username, BillPaymentRequestDTO request);
    void performSystemTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String memo);
    Optional<TransactionDetailDTO> getTransactionDetails(String username, Long transactionId);
    Optional<Biller> findBillerByInternalAccount(Account account);
}