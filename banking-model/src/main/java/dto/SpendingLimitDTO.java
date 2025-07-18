package dto;

import java.math.BigDecimal;

public class SpendingLimitDTO {
    private BigDecimal newLimit;

    public BigDecimal getNewLimit() {
        return newLimit;
    }
    public void setNewLimit(BigDecimal newLimit) {
        this.newLimit = newLimit;
    }
}