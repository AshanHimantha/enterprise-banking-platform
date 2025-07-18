package service;

import jakarta.ejb.Local;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@Local
public interface DocumentGenerationService {
    ByteArrayOutputStream generateAccountStatementPdf(String accountNumber, LocalDate startDate, LocalDate endDate);
    ByteArrayOutputStream generateTransactionReceiptPdf(String username, Long transactionId);
}