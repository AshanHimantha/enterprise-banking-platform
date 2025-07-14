package entity;

import enums.BillerCategory;
import enums.BillerStatus;
import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "biller")
public class Biller implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String billerName;

    public Long getId() {
        return id;
    }

    public BillerCategory getCategory() {
        return category;
    }

    public void setCategory(BillerCategory category) {
        this.category = category;
    }

    public BillerStatus getStatus() {
        return status;
    }

    public void setStatus(BillerStatus status) {
        this.status = status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillerName() {
        return billerName;
    }

    public void setBillerName(String billerName) {
        this.billerName = billerName;
    }

    @Enumerated(EnumType.STRING) // Stores the enum name (e.g., "UTILITIES") in the DB
    @Column(nullable = false)
    private BillerCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillerStatus status = BillerStatus.ACTIVE; // Default new billers to ACTIVE



    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Account getInternalAccount() {
        return internalAccount;
    }

    public void setInternalAccount(Account internalAccount) {
        this.internalAccount = internalAccount;
    }


    private String logoUrl;

    // The link to the internal account used to receive funds.
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_account_id", referencedColumnName = "id", nullable = false)
    private Account internalAccount;


}