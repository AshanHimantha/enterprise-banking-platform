package service;

import dto.TransactionRequestDTO;
import jakarta.ejb.Local;

@Local
public interface TransactionService {
    void performTransfer(String username, TransactionRequestDTO transactionRequest);
}