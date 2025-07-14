package service;


import dto.TransactionDTO;
import dto.TransactionRequestDTO;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface TransactionService {


    void performTransfer(String username, TransactionRequestDTO transactionRequest);
    List<TransactionDTO> getTransactionHistory(String username, String accountNumber);
}