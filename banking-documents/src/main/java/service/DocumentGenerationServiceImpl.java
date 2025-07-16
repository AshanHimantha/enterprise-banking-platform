package service;

import com.itextpdf.layout.properties.TextAlignment;
import entity.Account;
import entity.KycDocument;
import entity.Transaction;
import entity.User;
import enums.AccountType;
import enums.TransactionType;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Stateless
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    @Override
    public ByteArrayOutputStream generateAccountStatementPdf(String accountNumber, LocalDate startDate, LocalDate endDate) {
        Account account = findAccountByNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account with number " + accountNumber + " not found.");
        }

        if (account.getAccountType() != AccountType.SAVING && account.getAccountType() != AccountType.CURRENT) {
            System.err.println("Statement generation blocked for account " + accountNumber + " due to invalid account type: " + account.getAccountType());
            // Return an empty stream to indicate no statement should be generated.
            // Or you could throw an exception. Returning empty is safer for a batch process.
            return new ByteArrayOutputStream();
        }
        KycDocument kycDocument = findKycDocumentForUser(account.getOwner());
        List<Transaction> transactions = findTransactionsForStatement(account, startDate, endDate);

        try {
            InputStream templateStream = getClass().getClassLoader().getResourceAsStream("templates/statement-template.html");
            if (templateStream == null) {
                throw new RuntimeException("Critical Error: Could not find 'statement-template.html' in resources/templates folder.");
            }
            String htmlTemplate = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);

            String finalHtml = populateTemplate(htmlTemplate, account, transactions, startDate, endDate, kycDocument);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WriterProperties writerProperties = new WriterProperties();
            String userPassword = accountNumber.substring(accountNumber.length() - 4);
            writerProperties.setStandardEncryption(userPassword.getBytes(StandardCharsets.UTF_8), "owner-pass".getBytes(StandardCharsets.UTF_8),
                    EncryptionConstants.ALLOW_PRINTING, EncryptionConstants.ENCRYPTION_AES_128);

            PdfWriter pdfWriter = new PdfWriter(baos, writerProperties);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);

            // Register the event handler to add page numbers to the footer
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler());

            ConverterProperties converterProperties = new ConverterProperties();
            String baseUri = getClass().getClassLoader().getResource("templates/").toExternalForm();
            converterProperties.setBaseUri(baseUri);

            HtmlConverter.convertToPdf(finalHtml, pdfDoc, converterProperties); // Pass pdfDoc, not pdfWriter

            return baos;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF statement from template.", e);
        }
    }

    private String populateTemplate(String template, Account account, List<Transaction> transactions, LocalDate startDate, LocalDate endDate, KycDocument kycDocument) {
        // --- Header and Account Details ---
        template = template.replace("{{statement_period}}", startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
        template = template.replace("{{customer_name}}", (account.getOwner().getFirstName() + " " + account.getOwner().getLastName()).toUpperCase());

        String addressToDisplay;
        if (kycDocument != null && kycDocument.getAddress() != null) {
            addressToDisplay = kycDocument.getAddress(); // Use the verified KYC address
        } else {
            addressToDisplay = "Address Not Available";
        }
        template = template.replace("{{customer_address}}", addressToDisplay);

        template = template.replace("{{account_number}}", account.getAccountNumber());
        template = template.replace("{{account_type}}", account.getAccountType().toString() + " ACCOUNT");
        template = template.replace("{{statement_date}}", endDate.format(DATE_FORMATTER));

        // --- Account Summary Calculation ---
        BigDecimal openingBalance = calculateOpeningBalance(account, transactions);
        BigDecimal totalDeposits = calculateTotalByType(transactions, account, false);
        BigDecimal totalWithdrawals = calculateTotalByType(transactions, account, true);
        BigDecimal interestEarned = calculateTotalByType(transactions, TransactionType.INTEREST_PAYOUT);
        BigDecimal serviceCharges = calculateTotalByType(transactions, TransactionType.FEE);

        template = template.replace("{{beginning_balance_date}}", startDate.format(DATE_FORMATTER));
        template = template.replace("{{beginning_balance}}", "$" + CURRENCY_FORMAT.format(openingBalance));
        template = template.replace("{{total_deposits}}", "$" + CURRENCY_FORMAT.format(totalDeposits));
        template = template.replace("{{total_withdrawals}}", "$" + CURRENCY_FORMAT.format(totalWithdrawals)); // No brackets
        template = template.replace("{{service_charges}}", "$" + CURRENCY_FORMAT.format(serviceCharges));
        template = template.replace("{{interest_earned}}", "$" + CURRENCY_FORMAT.format(interestEarned));
        template = template.replace("{{ending_balance_date}}", endDate.format(DATE_FORMATTER));
        template = template.replace("{{ending_balance}}", "$" + CURRENCY_FORMAT.format(account.getBalance()));

        // --- Transaction Details Table Rows ---
        StringBuilder rowsHtml = new StringBuilder();
        if (transactions.isEmpty()) {
            rowsHtml.append("<tr><td colspan='5' style='text-align:center; padding: 1rem;'>No transactions during this period.</td></tr>");
        } else {
            for (Transaction tx : transactions) {
                rowsHtml.append("<tr>\n");
                rowsHtml.append("  <td>").append(tx.getTransactionDate().toLocalDate().format(DATE_FORMATTER)).append("</td>\n");
                rowsHtml.append("  <td>").append(tx.getDescription() != null ? tx.getDescription() : "").append("</td>\n");
                rowsHtml.append("  <td>").append(tx.getId()).append("</td>\n");

                if (isDebit(tx, account)) {
                    rowsHtml.append("  <td class='debit text-right'>").append(CURRENCY_FORMAT.format(tx.getAmount())).append("</td>\n"); // No brackets
                } else {
                    rowsHtml.append("  <td class='credit text-right'>").append(CURRENCY_FORMAT.format(tx.getAmount())).append("</td>\n");
                }

                rowsHtml.append("  <td class='text-right'>$").append(tx.getRunningBalance() != null ? CURRENCY_FORMAT.format(tx.getRunningBalance()) : "").append("</td>\n");
                rowsHtml.append("</tr>\n");
            }
        }

        return template.replace("{{transaction_rows}}", rowsHtml.toString());
    }

    // --- Helper Methods ---
    private KycDocument findKycDocumentForUser(User user) {
        try {
            return em.createQuery("SELECT k FROM KycDocument k WHERE k.user = :user", KycDocument.class)
                    .setParameter("user", user)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private boolean isDebit(Transaction tx, Account account) {
        // A transaction is a debit if the 'from' account is the user's account.
        // This includes TRANSFER, BILL_PAYMENT, and WITHDRAWAL types.
        return tx.getFromAccount() != null && tx.getFromAccount().equals(account);
    }

    private BigDecimal calculateOpeningBalance(Account account, List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return account.getBalance();
        }
        Transaction firstTx = transactions.get(0);
        BigDecimal firstTxAmount = firstTx.getAmount();
        BigDecimal runningBalance = firstTx.getRunningBalance();

        if (runningBalance == null) return account.getBalance(); // Fallback

        if (isDebit(firstTx, account)) {
            return runningBalance.add(firstTxAmount);
        } else {
            return runningBalance.subtract(firstTxAmount);
        }
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, Account account, boolean isDebit) {
        return transactions.stream()
                .filter(tx -> isDebit(tx, account) == isDebit)
                .filter(tx -> tx.getTransactionType() != TransactionType.INTEREST_PAYOUT) // Interest is not a deposit
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(tx -> tx.getTransactionType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Account findAccountByNumber(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
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

    // --- Static Inner Class for Page Number Event Handling ---
    protected static class PageNumberEventHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfDocument pdfDoc = docEvent.getDocument();
            PdfPage page = docEvent.getPage();
            int pageNumber = pdfDoc.getPageNumber(page);
            int numberOfPages = pdfDoc.getNumberOfPages();

            if (numberOfPages > 1) { // Only add page numbers if there is more than one page
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);

                new Canvas(pdfCanvas, pageSize)
                        .setFontSize(8)
                        .showTextAligned(
                                String.format("Page %d of %d", pageNumber, numberOfPages),
                                pageSize.getRight() - 30,
                                pageSize.getBottom() + 30,
                                TextAlignment.RIGHT
                        )
                        .close();
                pdfCanvas.release();
            }
        }
    }
}