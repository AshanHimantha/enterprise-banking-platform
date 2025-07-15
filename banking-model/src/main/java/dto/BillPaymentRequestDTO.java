package dto;


import java.math.BigDecimal; // Ensure this import is present

/**
 * A Data Transfer Object for initiating a new, one-time bill payment.
 * This class models the JSON request body sent by the client to the /api/bills/pay endpoint.
 */
public class BillPaymentRequestDTO {

    // The user's account number from which funds will be debited.
    private String fromAccountNumber;

    // The unique ID of the Biller (from the directory) that the user wants to pay.
    private Long billerId;

    // The user's specific account number or reference ID for that particular biller.
    private String billerReferenceNumber;

    // The amount to be paid. Using BigDecimal is crucial for financial accuracy.
    private BigDecimal amount;

    // An optional note or memo for the payment provided by the user.
    private String userMemo;

    /**
     * A no-argument constructor is required for JAX-RS and other frameworks
     * to instantiate the object when deserializing JSON.
     */
    public BillPaymentRequestDTO() {
    }

    // --- Getters and Setters ---
    // These are essential for the JSON mapping library to correctly populate the object.

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public Long getBillerId() {
        return billerId;
    }

    public void setBillerId(Long billerId) {
        this.billerId = billerId;
    }

    public String getBillerReferenceNumber() {
        return billerReferenceNumber;
    }

    public void setBillerReferenceNumber(String billerReferenceNumber) {
        this.billerReferenceNumber = billerReferenceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUserMemo() {
        return userMemo;
    }

    public void setUserMemo(String userMemo) {
        this.userMemo = userMemo;
    }
}