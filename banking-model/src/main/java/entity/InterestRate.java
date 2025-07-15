package entity;



import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "interest_rate")
public class InterestRate implements Serializable {

    @EmbeddedId
    private InterestRateId id;

    @Column(nullable = false, precision = 10, scale = 5)
    private BigDecimal annualRate;

    private String description;

    // --- Getters and Setters ---

    public InterestRateId getId() {
        return id;
    }

    public void setId(InterestRateId id) {
        this.id = id;
    }

    public BigDecimal getAnnualRate() {
        return annualRate;
    }

    public void setAnnualRate(BigDecimal annualRate) {
        this.annualRate = annualRate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}