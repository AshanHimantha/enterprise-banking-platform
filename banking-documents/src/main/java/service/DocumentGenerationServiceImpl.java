package service;

import entity.Account;
import entity.Transaction;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException; // Import this
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Stateless
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @Override
    public ByteArrayOutputStream generateAccountStatementPdf(String accountNumber, LocalDate startDate, LocalDate endDate) {
        // 1. Fetch Data
        Account account = findAccountByNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account with number " + accountNumber + " not found.");
        }

        List<Transaction> transactions = findTransactionsForStatement(account, startDate, endDate);
        System.out.println("Generating statement for account: " + accountNumber + " from " + startDate + " to " + endDate);

        if (transactions.isEmpty()) {
            System.out.println("No transactions found for account " + accountNumber + " in the given period.");
            return new ByteArrayOutputStream();
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // 2. Set up PDF Encryption
            WriterProperties writerProperties = new WriterProperties();
            String userPassword = accountNumber.substring(accountNumber.length() - 4);
            byte[] userPasswordBytes = userPassword.getBytes();
            byte[] ownerPasswordBytes = "a-very-strong-owner-password".getBytes();

            writerProperties.setStandardEncryption(userPasswordBytes, ownerPasswordBytes,
                    EncryptionConstants.ALLOW_PRINTING, EncryptionConstants.ENCRYPTION_AES_128);

            // 3. Create PDF Document
            PdfWriter pdfWriter = new PdfWriter(baos, writerProperties);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            Document document = new Document(pdfDoc);

            // 4. Add Content to the PDF
            document.add(new Paragraph("Orbin Bank - Account Statement").setBold().setFontSize(20));
            document.add(new Paragraph("Account Holder: " + account.getOwner().getFirstName() + " " + account.getOwner().getLastName()));
            document.add(new Paragraph("Account Number: " + account.getAccountNumber()));
            document.add(new Paragraph("Statement Period: " + startDate + " to " + endDate));
            document.add(new Paragraph("\n"));

            // Transaction Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 5, 2, 2, 3}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Date");
            table.addHeaderCell("Description");
            table.addHeaderCell("Debit");
            table.addHeaderCell("Credit");
            table.addHeaderCell("Balance");

            for (Transaction tx : transactions) {
                table.addCell(tx.getTransactionDate().toLocalDate().toString());
                table.addCell(tx.getDescription() != null ? tx.getDescription() : "");
                if (tx.getFromAccount() != null && tx.getFromAccount().equals(account)) {
                    table.addCell(tx.getAmount().toString());
                    table.addCell("");
                } else {
                    table.addCell("");
                    table.addCell(tx.getAmount().toString());
                }
                table.addCell(tx.getRunningBalance() != null ? tx.getRunningBalance().toString() : "");
            }

            document.add(table);
            document.close();

            return baos;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF statement.", e);
        }
    }

    // --- Helper Methods ---

    // *** THIS IS THE CORRECTED METHOD ***
    private Account findAccountByNumber(String accountNumber) {
        try {
            // Use JOIN FETCH to also retrieve the owner in the same query, which is more efficient.
            TypedQuery<Account> query = em.createQuery(
                    "SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class);
            query.setParameter("accountNumber", accountNumber);
            // Execute the query and return the single result.
            return query.getSingleResult();
        } catch (NoResultException e) {
            // If no account is found, return null or throw an exception.
            return null;
        }
    }

    private List<Transaction> findTransactionsForStatement(Account account, LocalDate startDate, LocalDate endDate) {
        TypedQuery<Transaction> query = em.createQuery(
                "SELECT t FROM Transaction t WHERE (t.fromAccount = :account OR t.toAccount = :account) " +
                        "AND t.transactionDate >= :startDateTime AND t.transactionDate < :endDateTime ORDER BY t.transactionDate ASC", Transaction.class);
        query.setParameter("account", account);
        query.setParameter("startDateTime", startDate.atStartOfDay());
        query.setParameter("endDateTime", endDate.plusDays(1).atStartOfDay());
        return query.getResultList();
    }
}