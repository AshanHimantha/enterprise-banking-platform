package service;


import entity.Account;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import mail.EmailService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/statementQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue")
})
public class StatementProcessorMDB implements MessageListener {

    @EJB
    private DocumentGenerationService documentGenerationService;

    @EJB
    private EmailService emailService;

    @EJB
    private AccountService accountService;

    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            try {
                TextMessage textMessage = (TextMessage) message;
                String payload = textMessage.getText();

                String[] parts = payload.split(";");
                if (parts.length < 3) {
                    System.err.println("MDB: Received invalid message format. Skipping. Payload: " + payload);
                    return;
                }
                String accountNumber = parts[0];
                LocalDate startDate = LocalDate.parse(parts[1]);
                LocalDate endDate = LocalDate.parse(parts[2]);

                System.out.println("MDB: Received job to generate statement for account " + accountNumber);

                ByteArrayOutputStream pdfStream = documentGenerationService.generateAccountStatementPdf(accountNumber, startDate, endDate);
                
                if (pdfStream != null && pdfStream.size() > 0) {

                    // Find the account to get the owner's email address
                    Account account = accountService.findAccountByNumber(accountNumber); // Using a placeholder method name

                    if (account == null || account.getOwner() == null) {
                        System.err.println("MDB: Could not find user for account number " + accountNumber + ". Cannot send email.");
                        return;
                    }

                    String userEmail = account.getOwner().getEmail();
                    String subject = "Your Account Statement for " + startDate.getMonth() + " " + startDate.getYear();
                    String body = String.format(
                            "<h3>Dear %s,</h3>" +
                                    "<p>Please find your account statement for the period of %s to %s attached.</p>" +
                                    "<p>The password to open the document is the last 4 digits of your account number Bank Account .</p>" +
                                    "<p>Example: <br> If your Account Number Is ORBIN-2025-123456 <br> Your Password will be : 3456 <br> Thank you,Orbin Bank</p>",
                            account.getOwner().getFirstName(),
                            startDate.toString(),
                            endDate.toString(),
                            accountNumber.substring(accountNumber.length() - 4)
                    );

                    // Call the real email service method
                    emailService.sendEmailWithAttachment(
                            userEmail,
                            subject,
                            body,
                            pdfStream.toByteArray(),
                            "Statement-" + startDate.getMonth() + "-" + startDate.getYear() + ".pdf",
                            "application/pdf"
                    );

                    System.out.println("MDB: Successfully processed and sent statement for account " + accountNumber);

                } else {
                    System.out.println("MDB: No transactions found for " + accountNumber + " in the period. Skipping email.");
                }

            } catch (Exception e) {
                // In a real production system, this error would cause the JMS broker to either
                // retry the message a few times or move it to a Dead Letter Queue (DLQ) for manual inspection.
                System.err.println("MDB: A critical error occurred while processing a statement message.");
                e.printStackTrace(); // Print the full error to the server log for debugging.
            }
        }
    }
}