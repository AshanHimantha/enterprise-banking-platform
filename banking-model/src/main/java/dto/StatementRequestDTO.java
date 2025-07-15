package dto;


import java.time.LocalDate;

/**
 * DTO for a user's on-demand request for an account statement.
 * This represents the JSON body the client will send.
 */
public class StatementRequestDTO {

    // The account number for which the statement is requested.
    private String accountNumber;

    // The start date of the desired statement period (format: YYYY-MM-DD).
    private LocalDate startDate;

    // The end date of the desired statement period (format: YYYY-MM-DD).
    private LocalDate endDate;

    // --- Getters and Setters are required ---

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}