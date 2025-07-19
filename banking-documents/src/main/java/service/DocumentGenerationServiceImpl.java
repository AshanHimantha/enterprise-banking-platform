package service;

import com.itextpdf.layout.properties.TextAlignment;
import dto.TransactionDetailDTO;
import entity.Account;
import entity.KycDocument;
import entity.Transaction;
import entity.User;
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

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private TransactionService transactionService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a");
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("$ #,##0.00");

    @Override
    public ByteArrayOutputStream generateAccountStatementPdf(String accountNumber, LocalDate startDate, LocalDate endDate) {
        Account account = findAccountByNumber(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account with number " + accountNumber + " not found.");
        }
        KycDocument kycDocument = findKycDocumentForUser(account.getOwner());
        List<Transaction> transactions = findTransactionsForStatement(account, startDate, endDate);

        try {
            InputStream templateStream = getTemplateAsStream("statement-template.html");
            String htmlTemplate = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
            String finalHtml = populateStatementTemplate(htmlTemplate, account, transactions, startDate, endDate, kycDocument);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WriterProperties writerProperties = new WriterProperties();
            String userPassword = accountNumber.substring(accountNumber.length() - 4);
            writerProperties.setStandardEncryption(userPassword.getBytes(StandardCharsets.UTF_8), "strong-owner-password".getBytes(StandardCharsets.UTF_8),
                    EncryptionConstants.ALLOW_PRINTING, EncryptionConstants.ENCRYPTION_AES_128);

            PdfWriter pdfWriter = new PdfWriter(baos, writerProperties);
            PdfDocument pdfDoc = new PdfDocument(pdfWriter);
            pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, new PageNumberEventHandler());

            ConverterProperties converterProperties = createConverterProperties();
            HtmlConverter.convertToPdf(finalHtml, pdfDoc, converterProperties);

            return baos;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF statement from template.", e);
        }
    }

    @Override
    public ByteArrayOutputStream generateTransactionReceiptPdf(String username, Long transactionId) {
        Optional<TransactionDetailDTO> dtoOptional = transactionService.getTransactionDetails(username, transactionId);
        if (dtoOptional.isEmpty()) {
            throw new SecurityException("Transaction not found or access denied.");
        }
        TransactionDetailDTO tx = dtoOptional.get();

        try {
            InputStream templateStream = getTemplateAsStream("receipt-template.html");
            String htmlTemplate = new String(templateStream.readAllBytes(), StandardCharsets.UTF_8);
            String finalHtml = populateReceiptTemplate(htmlTemplate, tx);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ConverterProperties converterProperties = createConverterProperties();
            HtmlConverter.convertToPdf(finalHtml, new PdfWriter(baos), converterProperties);

            return baos;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF receipt.", e);
        }
    }

    // --- Template Population Methods ---

    private String populateStatementTemplate(String template, Account account, List<Transaction> transactions, LocalDate startDate, LocalDate endDate, KycDocument kycDocument) {
        template = template.replace("{{statement_period}}", startDate.format(DATE_FORMATTER) + " - " + endDate.format(DATE_FORMATTER));
        template = template.replace("{{customer_name}}", (account.getOwner().getFirstName() + " " + account.getOwner().getLastName()).toUpperCase());

        String addressToDisplay = "Address Not Provided";
        if (kycDocument != null && kycDocument.getAddress() != null) {
            addressToDisplay = kycDocument.getAddress();
        } else if (account.getOwner().getAddress() != null) {
            addressToDisplay = account.getOwner().getAddress();
        }
        template = template.replace("{{customer_address}}", addressToDisplay);

        template = template.replace("{{account_number}}", "****-****-" + account.getAccountNumber().substring(account.getAccountNumber().length() - 4));
        template = template.replace("{{account_type}}", account.getAccountType().toString() + " ACCOUNT");
        template = template.replace("{{statement_date}}", endDate.format(DATE_FORMATTER));

        BigDecimal openingBalance = calculateOpeningBalance(account, transactions);
        BigDecimal totalDeposits = calculateTotalByType(transactions, account, false);
        BigDecimal totalWithdrawals = calculateTotalByType(transactions, account, true);
        BigDecimal interestEarned = calculateTotalByType(transactions, TransactionType.INTEREST_PAYOUT);
        BigDecimal serviceCharges = calculateTotalByType(transactions, TransactionType.FEE);

        template = template.replace("{{beginning_balance_date}}", startDate.format(DATE_FORMATTER));
        template = template.replace("{{beginning_balance}}", CURRENCY_FORMAT.format(openingBalance));
        template = template.replace("{{total_deposits}}", CURRENCY_FORMAT.format(totalDeposits));
        template = template.replace("{{total_withdrawals}}", CURRENCY_FORMAT.format(totalWithdrawals));
        template = template.replace("{{service_charges}}", CURRENCY_FORMAT.format(serviceCharges));
        template = template.replace("{{interest_earned}}", CURRENCY_FORMAT.format(interestEarned));
        template = template.replace("{{ending_balance_date}}", endDate.format(DATE_FORMATTER));
        template = template.replace("{{ending_balance}}", CURRENCY_FORMAT.format(account.getBalance()));

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
                    rowsHtml.append("  <td class='debit text-right'>").append(CURRENCY_FORMAT.format(tx.getAmount())).append("</td>\n");
                } else {
                    rowsHtml.append("  <td class='credit text-right'>").append(CURRENCY_FORMAT.format(tx.getAmount())).append("</td>\n");
                }
                rowsHtml.append("  <td class='text-right'>").append(tx.getRunningBalance() != null ? CURRENCY_FORMAT.format(tx.getRunningBalance()) : "").append("</td>\n");
                rowsHtml.append("</tr>\n");
            }
        }

        return template.replace("{{transaction_rows}}", rowsHtml.toString());
    }

    private String populateReceiptTemplate(String template, TransactionDetailDTO tx) {
        template = template.replace("{{payer_name}}", tx.getFromOwnerName() != null ? tx.getFromOwnerName().toUpperCase() : "N/A");
        template = template.replace("{{payer_account}}", tx.getFromAccountNumber());
        template = template.replace("{{recipient_name}}", tx.getToOwnerName() != null ? tx.getToOwnerName().toUpperCase() : "N/A");
        template = template.replace("{{recipient_account}}", tx.getToAccountNumber());
        template = template.replace("{{transaction_amount}}", CURRENCY_FORMAT.format(tx.getAmount()));
        template = template.replace("{{transaction_status}}", tx.getStatus().toString());
        template = template.replace("{{transaction_date}}", tx.getTransactionDate().format(DATE_FORMATTER));
        template = template.replace("{{transaction_time}}", tx.getTransactionDate().format(TIME_FORMATTER));
        template = template.replace("{{transaction_description}}", tx.getDescription() != null ? tx.getDescription() : "");
        template = template.replace("{{reference_number}}", tx.getId().toString());
        template = template.replace("{{transaction_type}}", tx.getTransactionType().toString().replace("_", " "));

        boolean isCredit = tx.getTransactionType() == TransactionType.DEPOSIT || tx.getTransactionType() == TransactionType.INTEREST_PAYOUT;
        template = template.replace("{{transaction_css_class}}", isCredit ? "credit" : "debit");

        return template;
    }

    // --- Helper Methods ---

    private ConverterProperties createConverterProperties() {
        ConverterProperties properties = new ConverterProperties();
        URL baseUriUrl = getClass().getClassLoader().getResource("templates/");
        if (baseUriUrl == null) {
            throw new RuntimeException("Critical configuration error: Could not find the 'templates' resource folder.");
        }
        properties.setBaseUri(baseUriUrl.toExternalForm());
        return properties;
    }

    private InputStream getTemplateAsStream(String templateName) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("templates/" + templateName);
        if (stream == null) {
            throw new RuntimeException("Critical Error: Could not find '" + templateName + "' in resources/templates folder.");
        }
        return stream;
    }

    private KycDocument findKycDocumentForUser(User user) {
        try {
            return em.createQuery("SELECT k FROM KycDocument k WHERE k.user = :user", KycDocument.class)
                    .setParameter("user", user).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private boolean isDebit(Transaction tx, Account account) {
        return tx.getFromAccount() != null && tx.getFromAccount().equals(account);
    }

    private BigDecimal calculateOpeningBalance(Account account, List<Transaction> transactions) {
        if (transactions.isEmpty()) return account.getBalance();
        Transaction firstTx = transactions.get(0);
        BigDecimal firstTxAmount = firstTx.getAmount();
        BigDecimal runningBalance = firstTx.getRunningBalance();
        if (runningBalance == null) return account.getBalance();
        return isDebit(firstTx, account) ? runningBalance.add(firstTxAmount) : runningBalance.subtract(firstTxAmount);
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, Account account, boolean isDebit) {
        return transactions.stream()
                .filter(tx -> isDebit(tx, account) == isDebit && tx.getTransactionType() != TransactionType.INTEREST_PAYOUT)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateTotalByType(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(tx -> tx.getTransactionType() == type)
                .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Account findAccountByNumber(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber).getSingleResult();
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

            if (numberOfPages > 0) {
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
                new Canvas(pdfCanvas, pageSize)
                        .setFontSize(8)
                        .showTextAligned(String.format("Page %d of %d", pageNumber, numberOfPages),
                                pageSize.getRight() - 30, pageSize.getBottom() + 20, TextAlignment.RIGHT)
                        .close();
                pdfCanvas.release();
            }
        }
    }
}