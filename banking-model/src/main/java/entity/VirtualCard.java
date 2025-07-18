package entity;


import enums.VirtualCardStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "virtual_card")
public class VirtualCard implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 16)
    private String cardNumber;
    @Column(nullable = false, length = 3)
    private String cvv;
    @Column(nullable = false)
    private LocalDate expiryDate;
    @Column(nullable = false)
    private String cardHolderName;
    private String nickname;

    @Column(nullable = true, length = 255)
    private String hashedPin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VirtualCardStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal spendingLimit;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin;
    }

    public VirtualCardStatus getStatus() {
        return status;
    }

    public void setStatus(VirtualCardStatus status) {
        this.status = status;
    }

    public BigDecimal getSpendingLimit() {
        return spendingLimit;
    }

    public void setSpendingLimit(BigDecimal spendingLimit) {
        this.spendingLimit = spendingLimit;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Account getLinkedAccount() {
        return linkedAccount;
    }

    public void setLinkedAccount(Account linkedAccount) {
        this.linkedAccount = linkedAccount;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_account_id", nullable = false)
    private Account linkedAccount;


}