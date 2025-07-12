package service;

import dto.KycDocumentDto;
import jakarta.ejb.Local;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

@Local
public interface KycService {

    void submitKyc(
            String username,
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            String idNumber,
            String address,
            String city,
            String postalCode,
            String country,
            InputStream idFrontPhotoStream,
            String idFrontPhotoFileName,
            InputStream idBackPhotoStream,
            String idBackPhotoFileName
    );

    // Methods for retrieving KYC data
    List<KycDocumentDto> getAllKycDocuments();

    List<KycDocumentDto> getKycDocumentsByStatus(String status);

    KycDocumentDto getKycDocumentByUsername(String username);

    KycDocumentDto getKycDocumentById(Long id);

    List<KycDocumentDto> getKycDocumentsPaginated(int page, int size);

    long getKycDocumentsCount();
}