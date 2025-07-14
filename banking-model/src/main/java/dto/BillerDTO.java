package dto;


import entity.Biller;
import enums.BillerCategory;
import enums.BillerStatus;

public class BillerDTO {
    private Long id;
    private String billerName;
    private BillerCategory category;
    private BillerStatus status;
    private String logoUrl;

    public BillerDTO() {}

    public BillerDTO(Biller biller) {
        this.id = biller.getId();
        this.billerName = biller.getBillerName();
        this.category = biller.getCategory();
        this.status = biller.getStatus();
        if (biller.getLogoUrl() != null && !biller.getLogoUrl().isEmpty()) {
            this.logoUrl = "/api/biller/logo/image/" + biller.getLogoUrl();
        } else {
            this.logoUrl = null;
        }
    }

    public Long getId() {
        return id;
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

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}