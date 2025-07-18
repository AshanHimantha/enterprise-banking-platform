package dto;

import entity.VirtualCard;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

public class UnmaskedVirtualCardDTO implements Serializable {

    private String cardNumber; // Full, unmasked 16-digit number
    private String cardHolderName;
    private String expiryDate; // Formatted as "MM/yy"
    private String cvv;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public UnmaskedVirtualCardDTO(VirtualCard card) {
        this.cardNumber = card.getCardNumber();
        this.cardHolderName = card.getCardHolderName();
        this.cvv = card.getCvv();

        if (card.getExpiryDate() != null) {
            this.expiryDate = card.getExpiryDate().format(DateTimeFormatter.ofPattern("MM/yy"));
        }
    }


}