package dto;

import enums.KycStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class KycDocumentDto {
    private Long id;
    private String username;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nationality;
    private String idNumber;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private LocalDateTime submittedAt;
    private KycStatus status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNotes;

    // Constructors
    public KycDocumentDto() {}

    public KycDocumentDto(Long id, String username, String fullName, LocalDate dateOfBirth,
                         String nationality, String idNumber, String address, String city,
                         String postalCode, String country, LocalDateTime submittedAt,
                         KycStatus status, String reviewedBy, LocalDateTime reviewedAt, String reviewNotes) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.idNumber = idNumber;
        this.address = address;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.submittedAt = submittedAt;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.reviewNotes = reviewNotes;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public KycStatus getStatus() { return status; }
    public void setStatus(KycStatus status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}
