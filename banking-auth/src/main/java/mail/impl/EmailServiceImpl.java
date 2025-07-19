package mail.impl;



import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import mail.EmailService;
import util.LoggingInterceptor;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class EmailServiceImpl implements EmailService {


    @Resource(name = "mail/zoho")
    private Session mailSession;

    @Override
    @Asynchronous // Keep this for performance!
    public void sendVerificationEmail(String recipientEmail, String username, String verificationCode) {
        try {
            Message message = new MimeMessage(mailSession);
            // The "From" address is automatically set from the mail session's "Default User"
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Welcome to Orbin Bank! Please Verify Your Email");

            String emailContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                    "<meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<title>Welcome to Orbin Bank</title>" +
                    "<style>" +
                        "* { box-sizing: border-box; }" +
                        "body {" +
                            "margin: 0;" +
                            "padding: 20px;" +
                            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                            "background-color: #f5f5f5;" +
                            "line-height: 1.6;" +
                        "}" +
                        ".email-container {" +
                            "max-width: 600px;" +
                            "margin: 0 auto;" +
                            "background: #ffffff;" +
                            "border-radius: 12px;" +
                            "overflow: hidden;" +
                            "border: 1px solid #e9ecef;" +
                        "}" +
                        ".header {" +
                            "background: linear-gradient(135deg, #ff8c00 0%, #ff9500 100%);" +
                            "padding: 30px 20px;" +
                            "text-align: center;" +
                            "color: white;" +
                        "}" +
                        ".logo {" +
                            "max-width: 80px;" +
                            "height: auto;" +
                            "margin-bottom: 15px;" +
                            "border-radius: 8px;" +
                    "background: #fffff1;" +

                        "}" +
                        ".header h1 {" +
                            "margin: 0;" +
                            "font-size: 24px;" +
                            "font-weight: 600;" +
                            "color: white !important;" +
                        "}" +
                        ".content {" +
                            "padding: 40px 30px;" +
                            "background: #ffffff;" +
                        "}" +
                        ".greeting {" +
                            "font-size: 20px;" +
                            "color: #333333;" +
                            "margin: 0 0 20px 0;" +
                            "font-weight: 500;" +
                        "}" +
                        ".message {" +
                            "font-size: 16px;" +
                            "color: #555555;" +
                            "margin: 0 0 25px 0;" +
                            "line-height: 1.7;" +
                        "}" +
                        ".verification-section {" +
                            "background: #f8f9fa;" +
                            "border: 2px solid #e9ecef;" +
                            "border-radius: 10px;" +
                            "padding: 25px;" +
                            "text-align: center;" +
                            "margin: 30px 0;" +
                        "}" +
                        ".verification-title {" +
                            "font-size: 16px;" +
                            "color: #333333;" +
                            "margin: 0 0 15px 0;" +
                            "font-weight: 600;" +
                        "}" +
                        ".verification-code {" +
                            "background: #ff8c00 !important;" +
                            "color: white !important;" +
                            "font-size: 28px;" +
                            "font-weight: 700;" +
                            "padding: 15px 30px;" +
                            "border-radius: 8px;" +
                            "display: inline-block;" +
                            "letter-spacing: 4px;" +
                            "margin: 10px 0;" +
                            "min-width: 200px;" +
                            "border: 2px solid #ff8c00;" +
                        "}" +
                        ".instructions {" +
                            "font-size: 14px;" +
                            "color: #666666;" +
                            "margin: 20px 0 0 0;" +
                            "padding: 15px;" +
                            "background: #fff3cd;" +
                            "border: 1px solid #ffeaa7;" +
                            "border-radius: 6px;" +
                        "}" +
                        ".security-info {" +
                            "background: #d4edda;" +
                            "border: 1px solid #c3e6cb;" +
                            "border-radius: 6px;" +
                            "padding: 15px;" +
                            "margin: 20px 0;" +
                            "font-size: 14px;" +
                            "color: #155724;" +
                        "}" +
                        ".footer {" +
                            "background: #f8f9fa;" +
                            "padding: 25px 30px;" +
                            "text-align: center;" +
                            "border-top: 1px solid #e9ecef;" +
                        "}" +
                        ".footer-text {" +
                            "color: #6c757d;" +
                            "font-size: 12px;" +
                            "margin: 0;" +
                            "line-height: 1.5;" +
                        "}" +
                        ".highlight {" +
                            "color: #ff8c00;" +
                            "font-weight: 600;" +
                        "}" +
                        "/* Dark Mode Support */" +
                        "@media (prefers-color-scheme: dark) {" +
                            "body { background-color: #1a1a1a !important; }" +
                            ".email-container { background: #2d2d2d !important; border: 1px solid #404040 !important; }" +
                            ".content { background: #2d2d2d !important; }" +
                            ".greeting { color: #e0e0e0 !important; }" +
                            ".message { color: #cccccc !important; }" +
                            ".verification-section { background: #3a3a3a !important; border: 2px solid #505050 !important; }" +
                            ".verification-title { color: #e0e0e0 !important; }" +
                            ".instructions { background: #4a4a3a !important; border: 1px solid #666633 !important; color: #e0e0e0 !important; }" +
                            ".security-info { background: #2a3a2a !important; border: 1px solid #336633 !important; color: #ccffcc !important; }" +
                            ".footer { background: #383838 !important; border-top: 1px solid #505050 !important; }" +
                            ".footer-text { color: #aaaaaa !important; }" +
                        "}" +
                        "/* Force light mode for email clients that don't handle dark mode well */" +
                        "[data-ogsc] .email-container { background: #ffffff !important; }" +
                        "[data-ogsc] .content { background: #ffffff !important; }" +
                        "[data-ogsc] .greeting { color: #333333 !important; }" +
                        "[data-ogsc] .message { color: #555555 !important; }" +
                        "/* Mobile responsiveness */" +
                        "@media (max-width: 600px) {" +
                            "body { padding: 10px; }" +
                            ".content { padding: 25px 20px; }" +
                            ".verification-code { font-size: 24px; padding: 12px 20px; letter-spacing: 2px; min-width: 180px; }" +
                            ".header h1 { font-size: 20px; }" +
                            ".greeting { font-size: 18px; }" +
                        "}" +
                    "</style>" +
                "</head>" +
                "<body>" +
                    "<div class=\"email-container\">" +
                        "<div class=\"header\">" +
                            "<img src=\"https://ashan.1000dtechnology.com/assets/orbin2.jpg\" alt=\"Orbin Bank\" class=\"logo\">" +
                            "<h1>Welcome to Orbin Bank</h1>" +
                        "</div>" +
                        "<div class=\"content\">" +
                            "<h2 class=\"greeting\">Hello " + username + "! 👋</h2>" +
                            "<p class=\"message\">" +
                                "Thank you for choosing <span class=\"highlight\">Orbin Bank</span>. We're excited to have you as part of our banking family!" +
                            "</p>" +
                            "<p class=\"message\">" +
                                "To complete your account setup and ensure your security, please verify your email address using the code below:" +
                            "</p>" +
                            "<div class=\"verification-section\">" +
                                "<div class=\"verification-title\">📧 Email Verification Code</div>" +
                                "<div class=\"verification-code\">" + verificationCode + "</div>" +
                                "<div class=\"instructions\">" +
                                    "⚠️ <strong>Important:</strong> Enter this code exactly as shown to verify your email address." +
                                "</div>" +
                            "</div>" +
                            "<div class=\"security-info\">" +
                                "🔒 <strong>Security Notice:</strong><br>" +
                                "• This code expires in <strong>15 minutes</strong><br>" +
                                "• Never share this code with anyone<br>" +
                                "• If you didn't request this, please ignore this email" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "Need help? Contact our support team at <span class=\"highlight\">orbinbank@ashanhimantha321.com</span>" +
                            "</p>" +
                        "</div>" +
                        "<div class=\"footer\">" +
                            "<p class=\"footer-text\">" +
                                "© 2025 Orbin Bank. All rights reserved.<br>" +
                                "This is an automated message. Please do not reply to this email." +
                            "</p>" +
                        "</div>" +
                    "</div>" +
                "</body>" +
                "</html>";

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Verification email sent successfully to " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("Error sending email via SMTP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Asynchronous
    public void sendLoginVerificationCode(String recipientEmail, String username, String verificationCode) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Orbin Bank - Login Verification Code");

            String emailContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                    "<meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<title>Login Verification - Orbin Bank</title>" +
                    "<style>" +
                        "* { box-sizing: border-box; }" +
                        "body {" +
                            "margin: 0;" +
                            "padding: 20px;" +
                            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                            "background-color: #f5f5f5;" +
                            "line-height: 1.6;" +
                        "}" +
                        ".email-container {" +
                            "max-width: 600px;" +
                            "margin: 0 auto;" +
                            "background: #ffffff;" +
                            "border-radius: 12px;" +
                            "overflow: hidden;" +
                            "border: 1px solid #e9ecef;" +
                        "}" +
                        ".header {" +
                            "background: linear-gradient(135deg, #ff8c00 0%, #ff9500 100%);" +
                            "padding: 30px 20px;" +
                            "text-align: center;" +
                            "color: white;" +
                        "}" +
                        ".logo {" +
                            "max-width: 80px;" +
                            "height: auto;" +
                            "margin-bottom: 15px;" +
                            "border-radius: 8px;" +
                            "background: #fffff1;" +
                        "}" +
                        ".header h1 {" +
                            "margin: 0;" +
                            "font-size: 24px;" +
                            "font-weight: 600;" +
                            "color: white !important;" +
                        "}" +
                        ".content {" +
                            "padding: 40px 30px;" +
                            "background: #ffffff;" +
                        "}" +
                        ".greeting {" +
                            "font-size: 20px;" +
                            "color: #333333;" +
                            "margin: 0 0 20px 0;" +
                            "font-weight: 500;" +
                        "}" +
                        ".message {" +
                            "font-size: 16px;" +
                            "color: #555555;" +
                            "margin: 0 0 25px 0;" +
                            "line-height: 1.7;" +
                        "}" +
                        ".verification-section {" +
                            "background: #f8f9fa;" +
                            "border: 2px solid #e9ecef;" +
                            "border-radius: 10px;" +
                            "padding: 25px;" +
                            "text-align: center;" +
                            "margin: 30px 0;" +
                        "}" +
                        ".verification-title {" +
                            "font-size: 16px;" +
                            "color: #333333;" +
                            "margin: 0 0 15px 0;" +
                            "font-weight: 600;" +
                        "}" +
                        ".verification-code {" +
                            "background: #ff8c00 !important;" +
                            "color: white !important;" +
                            "font-size: 28px;" +
                            "font-weight: 700;" +
                            "padding: 15px 30px;" +
                            "border-radius: 8px;" +
                            "display: inline-block;" +
                            "letter-spacing: 4px;" +
                            "margin: 10px 0;" +
                            "min-width: 200px;" +
                            "border: 2px solid #ff8c00;" +
                        "}" +
                        ".instructions {" +
                            "font-size: 14px;" +
                            "color: #666666;" +
                            "margin: 20px 0 0 0;" +
                            "padding: 15px;" +
                            "background: #fff3cd;" +
                            "border: 1px solid #ffeaa7;" +
                            "border-radius: 6px;" +
                        "}" +
                        ".security-info {" +
                            "background: #d4edda;" +
                            "border: 1px solid #c3e6cb;" +
                            "border-radius: 6px;" +
                            "padding: 15px;" +
                            "margin: 20px 0;" +
                            "font-size: 14px;" +
                            "color: #155724;" +
                        "}" +
                        ".footer {" +
                            "background: #f8f9fa;" +
                            "padding: 25px 30px;" +
                            "text-align: center;" +
                            "border-top: 1px solid #e9ecef;" +
                        "}" +
                        ".footer-text {" +
                            "color: #6c757d;" +
                            "font-size: 12px;" +
                            "margin: 0;" +
                            "line-height: 1.5;" +
                        "}" +
                        ".highlight {" +
                            "color: #ff8c00;" +
                            "font-weight: 600;" +
                        "}" +
                    "</style>" +
                "</head>" +
                "<body>" +
                    "<div class=\"email-container\">" +
                        "<div class=\"header\">" +
                            "<img src=\"https://ashan.1000dtechnology.com/assets/orbin2.jpg\" alt=\"Orbin Bank\" class=\"logo\">" +
                            "<h1>Login Verification</h1>" +
                        "</div>" +
                        "<div class=\"content\">" +
                            "<h2 class=\"greeting\">Hello " + username + "! 🔐</h2>" +
                            "<p class=\"message\">" +
                                "We detected a login attempt for your <span class=\"highlight\">Orbin Bank</span> account. For your security, please use the verification code below to complete your login:" +
                            "</p>" +
                            "<div class=\"verification-section\">" +
                                "<div class=\"verification-title\">🔑 Login Verification Code</div>" +
                                "<div class=\"verification-code\">" + verificationCode + "</div>" +
                                "<div class=\"instructions\">" +
                                    "⚠️ <strong>Important:</strong> Enter this code in your login screen to access your account." +
                                "</div>" +
                            "</div>" +
                            "<div class=\"security-info\">" +
                                "🔒 <strong>Security Notice:</strong><br>" +
                                "• This code expires in <strong>10 minutes</strong><br>" +
                                "• Never share this code with anyone<br>" +
                                "• If you didn't attempt to log in, please contact us immediately" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "If you didn't request this login, please contact our security team at <span class=\"highlight\">security@orbinbank.com</span>" +
                            "</p>" +
                        "</div>" +
                        "<div class=\"footer\">" +
                            "<p class=\"footer-text\">" +
                                "© 2025 Orbin Bank. All rights reserved.<br>" +
                                "This is an automated message. Please do not reply to this email." +
                            "</p>" +
                        "</div>" +
                    "</div>" +
                "</body>" +
                "</html>";

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Login verification email sent successfully to " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("Error sending login verification email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Asynchronous
    public void sendAccountSuspensionEmail(String recipientEmail, String username, String reason, String adminUsername) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Account Suspension Notice - Orbin Bank");

            String emailContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                    "<meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<title>Account Suspension Notice - Orbin Bank</title>" +
                    "<style>" +
                        "* { box-sizing: border-box; }" +
                        "body {" +
                            "margin: 0;" +
                            "padding: 20px;" +
                            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                            "background-color: #f5f5f5;" +
                            "line-height: 1.6;" +
                        "}" +
                        ".email-container {" +
                            "max-width: 600px;" +
                            "margin: 0 auto;" +
                            "background: #ffffff;" +
                            "border-radius: 12px;" +
                            "overflow: hidden;" +
                            "border: 1px solid #e9ecef;" +
                        "}" +
                        ".header {" +
                            "background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);" +
                            "padding: 30px 20px;" +
                            "text-align: center;" +
                            "color: white;" +
                        "}" +
                        ".logo {" +
                            "max-width: 80px;" +
                            "height: auto;" +
                            "margin-bottom: 15px;" +
                            "border-radius: 8px;" +
                            "background: #fffff1;" +
                        "}" +
                        ".header h1 {" +
                            "margin: 0;" +
                            "font-size: 24px;" +
                            "font-weight: 600;" +
                            "color: white !important;" +
                        "}" +
                        ".content {" +
                            "padding: 40px 30px;" +
                            "background: #ffffff;" +
                        "}" +
                        ".greeting {" +
                            "font-size: 20px;" +
                            "color: #333333;" +
                            "margin: 0 0 20px 0;" +
                            "font-weight: 500;" +
                        "}" +
                        ".message {" +
                            "font-size: 16px;" +
                            "color: #555555;" +
                            "margin: 0 0 25px 0;" +
                            "line-height: 1.7;" +
                        "}" +
                        ".suspension-section {" +
                            "background: #f8d7da;" +
                            "border: 2px solid #f5c6cb;" +
                            "border-radius: 10px;" +
                            "padding: 25px;" +
                            "margin: 30px 0;" +
                        "}" +
                        ".suspension-title {" +
                            "font-size: 18px;" +
                            "color: #721c24;" +
                            "margin: 0 0 15px 0;" +
                            "font-weight: 600;" +
                        "}" +
                        ".reason-box {" +
                            "background: #ffffff;" +
                            "border: 1px solid #f5c6cb;" +
                            "border-radius: 8px;" +
                            "padding: 15px;" +
                            "margin: 15px 0;" +
                            "font-size: 14px;" +
                            "color: #721c24;" +
                        "}" +
                        ".highlight {" +
                            "color: #dc3545;" +
                            "font-weight: 600;" +
                        "}" +
                        ".contact-info {" +
                            "background: #d4edda;" +
                            "border: 1px solid #c3e6cb;" +
                            "border-radius: 6px;" +
                            "padding: 15px;" +
                            "margin: 20px 0;" +
                            "font-size: 14px;" +
                            "color: #155724;" +
                        "}" +
                        ".footer {" +
                            "background: #f8f9fa;" +
                            "padding: 25px 30px;" +
                            "text-align: center;" +
                            "border-top: 1px solid #e9ecef;" +
                        "}" +
                        ".footer-text {" +
                            "color: #6c757d;" +
                            "font-size: 12px;" +
                            "margin: 0;" +
                            "line-height: 1.5;" +
                        "}" +
                    "</style>" +
                "</head>" +
                "<body>" +
                    "<div class=\"email-container\">" +
                        "<div class=\"header\">" +
                            "<img src=\"https://ashan.1000dtechnology.com/assets/orbin2.jpg\" alt=\"Orbin Bank\" class=\"logo\">" +
                            "<h1>Account Suspension Notice</h1>" +
                        "</div>" +
                        "<div class=\"content\">" +
                            "<h2 class=\"greeting\">Dear " + username + ",</h2>" +
                            "<p class=\"message\">" +
                                "We are writing to inform you that your <span class=\"highlight\">Orbin Bank</span> account has been suspended effective immediately." +
                            "</p>" +
                            "<div class=\"suspension-section\">" +
                                "<div class=\"suspension-title\">🚫 Account Suspension Details</div>" +
                                "<div class=\"reason-box\">" +
                                    "<strong>Reason for Suspension:</strong><br>" +
                                    reason +
                                "</div>" +

                                "<p><strong>Date:</strong> " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")) + "</p>" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "During the suspension period, you will not be able to access your account or perform any banking transactions. " +
                                "All services including online banking, mobile app access, and card transactions are temporarily disabled." +
                            "</p>" +
                            "<div class=\"contact-info\">" +
                                "📞 <strong>Need to Appeal or Have Questions?</strong><br>" +
                                "If you believe this suspension is in error or would like to discuss your account status, please contact our customer service team:<br><br>" +
                                "• Phone: +94 701705553<br>" +
                                "• Email: <span class=\"highlight\">orbinbank@ashanhimantha321.com</span><br>" +
                                "• Visit any Orbin Bank branch with valid identification" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "We take account security and compliance very seriously. This action was taken to protect your account and maintain the integrity of our banking system." +
                            "</p>" +
                        "</div>" +
                        "<div class=\"footer\">" +
                            "<p class=\"footer-text\">" +
                                "© 2025 Orbin Bank. All rights reserved.<br>" +
                                "This is an automated message. Please do not reply to this email." +
                            "</p>" +
                        "</div>" +
                    "</div>" +
                "</body>" +
                "</html>";

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Account suspension email sent successfully to " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("Error sending account suspension email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    @Asynchronous
    public void sendEmailWithAttachment(String recipientEmail, String subject, String body, byte[] attachmentData, String attachmentName, String attachmentType) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);

            // 1. Create the body part for the HTML text
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(body, "text/html; charset=utf-8");

            // 2. Create the body part for the PDF attachment
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(attachmentData, attachmentType);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachmentName);

            // 3. Combine them into a multipart message
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            // 4. Set the multipart content on the main message
            message.setContent(multipart);

            Transport.send(message);
            System.out.println("Email with attachment sent successfully to " + recipientEmail);

        } catch (MessagingException e) {
            throw new RuntimeException("Error sending email with attachment", e);
        }
    }

    @Override
    @Asynchronous
    public void sendAccountReactivationEmail(String recipientEmail, String username, String adminUsername) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Account Reactivation Notice - Orbin Bank");

            String emailContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                    "<meta charset=\"UTF-8\">" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                    "<title>Account Reactivation Notice - Orbin Bank</title>" +
                    "<style>" +
                        "* { box-sizing: border-box; }" +
                        "body {" +
                            "margin: 0;" +
                            "padding: 20px;" +
                            "font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;" +
                            "background-color: #f5f5f5;" +
                            "line-height: 1.6;" +
                        "}" +
                        ".email-container {" +
                            "max-width: 600px;" +
                            "margin: 0 auto;" +
                            "background: #ffffff;" +
                            "border-radius: 12px;" +
                            "overflow: hidden;" +
                            "border: 1px solid #e9ecef;" +
                        "}" +
                        ".header {" +
                            "background: linear-gradient(135deg, #28a745 0%, #20c997 100%);" +
                            "padding: 30px 20px;" +
                            "text-align: center;" +
                            "color: white;" +
                        "}" +
                        ".logo {" +
                            "max-width: 80px;" +
                            "height: auto;" +
                            "margin-bottom: 15px;" +
                            "border-radius: 8px;" +
                            "background: #fffff1;" +
                        "}" +
                        ".header h1 {" +
                            "margin: 0;" +
                            "font-size: 24px;" +
                            "font-weight: 600;" +
                            "color: white !important;" +
                        "}" +
                        ".content {" +
                            "padding: 40px 30px;" +
                            "background: #ffffff;" +
                        "}" +
                        ".greeting {" +
                            "font-size: 20px;" +
                            "color: #333333;" +
                            "margin: 0 0 20px 0;" +
                            "font-weight: 500;" +
                        "}" +
                        ".message {" +
                            "font-size: 16px;" +
                            "color: #555555;" +
                            "margin: 0 0 25px 0;" +
                            "line-height: 1.7;" +
                        "}" +
                        ".reactivation-section {" +
                            "background: #d4edda;" +
                            "border: 2px solid #c3e6cb;" +
                            "border-radius: 10px;" +
                            "padding: 25px;" +
                            "margin: 30px 0;" +
                        "}" +
                        ".reactivation-title {" +
                            "font-size: 18px;" +
                            "color: #155724;" +
                            "margin: 0 0 15px 0;" +
                            "font-weight: 600;" +
                        "}" +
                        ".highlight {" +
                            "color: #28a745;" +
                            "font-weight: 600;" +
                        "}" +
                        ".services-info {" +
                            "background: #fff3cd;" +
                            "border: 1px solid #ffeaa7;" +
                            "border-radius: 6px;" +
                            "padding: 15px;" +
                            "margin: 20px 0;" +
                            "font-size: 14px;" +
                            "color: #856404;" +
                        "}" +
                        ".footer {" +
                            "background: #f8f9fa;" +
                            "padding: 25px 30px;" +
                            "text-align: center;" +
                            "border-top: 1px solid #e9ecef;" +
                        "}" +
                        ".footer-text {" +
                            "color: #6c757d;" +
                            "font-size: 12px;" +
                            "margin: 0;" +
                            "line-height: 1.5;" +
                        "}" +
                    "</style>" +
                "</head>" +
                "<body>" +
                    "<div class=\"email-container\">" +
                        "<div class=\"header\">" +
                            "<img src=\"https://ashan.1000dtechnology.com/assets/orbin2.jpg\" alt=\"Orbin Bank\" class=\"logo\">" +
                            "<h1>Account Reactivation Notice</h1>" +
                        "</div>" +
                        "<div class=\"content\">" +
                            "<h2 class=\"greeting\">Dear " + username + ",</h2>" +
                            "<p class=\"message\">" +
                                "We are pleased to inform you that your <span class=\"highlight\">Orbin Bank</span> account has been successfully reactivated!" +
                            "</p>" +
                            "<div class=\"reactivation-section\">" +
                                "<div class=\"reactivation-title\">✅ Account Reactivation Details</div>" +

                                "<p><strong>Date:</strong> " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")) + "</p>" +
                                "<p><strong>Status:</strong> <span class=\"highlight\">Active</span></p>" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "Your account is now fully operational, and you can resume all banking activities including:" +
                            "</p>" +
                            "<div class=\"services-info\">" +
                                "🏦 <strong>Available Services:</strong><br>" +
                                "• Online and mobile banking access<br>" +
                                "• Debit and credit card transactions<br>" +
                                "• Fund transfers and payments<br>" +
                                "• ATM withdrawals and deposits<br>" +
                                "• All other banking services" +
                            "</div>" +
                            "<p class=\"message\">" +
                                "We appreciate your patience during the suspension period. If you have any questions or need assistance accessing your account, " +
                                "please don't hesitate to contact our customer service team at <span class=\"highlight\">orbinbank@ashanhimantha321.com</span> or call +94 701705553." +
                            "</p>" +
                            "<p class=\"message\">" +
                                "Thank you for banking with Orbin Bank. We look forward to continuing to serve your financial needs." +
                            "</p>" +
                        "</div>" +
                        "<div class=\"footer\">" +
                            "<p class=\"footer-text\">" +
                                "© 2025 Orbin Bank. All rights reserved.<br>" +
                                "This is an automated message. Please do not reply to this email." +
                            "</p>" +
                        "</div>" +
                    "</div>" +
                "</body>" +
                "</html>";

            message.setContent(emailContent, "text/html; charset=utf-8");

            Transport.send(message);

            System.out.println("Account reactivation email sent successfully to " + recipientEmail);

        } catch (MessagingException e) {
            System.err.println("Error sending account reactivation email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}