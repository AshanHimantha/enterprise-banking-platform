package service;

import dto.KycDocumentDto;
import entity.KycDocument;
import entity.User;
import enums.KycStatus;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.NoResultException;
import jakarta.servlet.ServletContext;
import jakarta.annotation.Resource;
import util.LoggingInterceptor;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class KycServiceImpl implements KycService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // Method to get the webapp's real path for file uploads
    private String getWebappKycDirectory() {
        // This will resolve to the actual webapp directory in the deployed application
        // For development: typically target/banking-web-1.0/assets/kyc/
        // For production: the deployed WAR's assets/kyc/ directory
        String webappPath = System.getProperty("com.sun.aas.instanceRoot");
        if (webappPath != null) {
            // Payara/GlassFish specific path
            return webappPath + "/applications/banking-ear/assets/kyc/";
        } else {
            // Fallback to a local directory
            return "C:\\banking_uploads\\kyc_images\\";
        }
    }

    @Override
    public void submitKyc(
            String username, String fullName, LocalDate dateOfBirth, String nationality,
            String idNumber, String address, String city, String postalCode, String country,
            InputStream idFrontPhotoStream, String idFrontPhotoFileName,
            InputStream idBackPhotoStream, String idBackPhotoFileName) {

        // Find the user
        TypedQuery<User> userQuery = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        userQuery.setParameter("username", username);
        User user = userQuery.getSingleResult();

        if (user.getKycStatus() == KycStatus.VERIFIED) {
            throw new IllegalStateException("KYC is already verified for this user.");
        }

        try {
            // 1. Save the uploaded files to the server's file system
            String frontPhotoSavedName = saveFile(idFrontPhotoStream, idFrontPhotoFileName, username);
            String backPhotoSavedName = saveFile(idBackPhotoStream, idBackPhotoFileName, username);

            // 2. Create and populate the KycDocument entity
            KycDocument kycDoc = new KycDocument();
            kycDoc.setUser(user);
            kycDoc.setFullName(fullName);
            kycDoc.setDateOfBirth(dateOfBirth);
            kycDoc.setNationality(nationality);
            kycDoc.setIdNumber(idNumber);
            kycDoc.setAddress(address);
            kycDoc.setCity(city);
            kycDoc.setPostalCode(postalCode);
            kycDoc.setCountry(country);
            kycDoc.setIdFrontPhotoPath(frontPhotoSavedName);
            kycDoc.setIdBackPhotoPath(backPhotoSavedName);
            kycDoc.setSubmittedAt(LocalDateTime.now());

            // 3. Save the KYC document record to the database
            em.persist(kycDoc);

            // 4. Update the user's status to indicate KYC is pending review
            // (It's already PENDING by default, but this is explicit)
            user.setKycStatus(KycStatus.PENDING);
            em.merge(user);

        } catch (Exception e) {
            // In a real app, you would have rollback logic for the saved files
            throw new RuntimeException("Failed to process KYC submission.", e);
        }
    }

    private String saveFile(InputStream inputStream, String originalFileName, String username) throws Exception {
        // Create a unique filename to avoid conflicts
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = username + "_" + UUID.randomUUID().toString() + extension;

        // Ensure the upload directory exists
        File uploadDir = new File(getWebappKycDirectory());
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        java.nio.file.Path destination = Paths.get(getWebappKycDirectory() + uniqueFileName);
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);

        return destination.toString(); // Return the full path to store in the DB
    }

    @Override
    public List<KycDocumentDto> getAllKycDocuments() {
        TypedQuery<KycDocument> query = em.createQuery(
            "SELECT k FROM KycDocument k JOIN k.user u ORDER BY k.submittedAt DESC",
            KycDocument.class
        );
        List<KycDocument> documents = query.getResultList();
        return convertToDto(documents);
    }

    @Override
    public List<KycDocumentDto> getKycDocumentsByStatus(String status) {
        try {
            KycStatus kycStatus = KycStatus.valueOf(status.toUpperCase());
            TypedQuery<KycDocument> query = em.createQuery(
                "SELECT k FROM KycDocument k JOIN k.user u WHERE u.kycStatus = :status ORDER BY k.submittedAt DESC",
                KycDocument.class
            );
            query.setParameter("status", kycStatus);
            List<KycDocument> documents = query.getResultList();
            return convertToDto(documents);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid KYC status: " + status);
        }
    }

    @Override
    public KycDocumentDto getKycDocumentByUsername(String username) {
        try {
            TypedQuery<KycDocument> query = em.createQuery(
                "SELECT k FROM KycDocument k JOIN k.user u WHERE u.username = :username",
                KycDocument.class
            );
            query.setParameter("username", username);
            KycDocument document = query.getSingleResult();
            return convertToDto(document);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public KycDocumentDto getKycDocumentById(Long id) {
        try {
            TypedQuery<KycDocument> query = em.createQuery(
                "SELECT k FROM KycDocument k JOIN k.user u WHERE k.id = :id",
                KycDocument.class
            );
            query.setParameter("id", id);
            KycDocument document = query.getSingleResult();
            return convertToDto(document);
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public List<KycDocumentDto> getKycDocumentsPaginated(int page, int size) {
        TypedQuery<KycDocument> query = em.createQuery(
            "SELECT k FROM KycDocument k JOIN k.user u ORDER BY k.submittedAt DESC",
            KycDocument.class
        );
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<KycDocument> documents = query.getResultList();
        return convertToDto(documents);
    }

    @Override
    public long getKycDocumentsCount() {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(k) FROM KycDocument k",
            Long.class
        );
        return query.getSingleResult();
    }

    private List<KycDocumentDto> convertToDto(List<KycDocument> documents) {
        List<KycDocumentDto> dtoList = new ArrayList<>();
        for (KycDocument document : documents) {
            dtoList.add(convertToDto(document));
        }
        return dtoList;
    }

    private KycDocumentDto convertToDto(KycDocument document) {
        User user = document.getUser();
        return new KycDocumentDto(
            document.getId(),
            user.getUsername(),
            document.getFullName(),
            document.getDateOfBirth(),
            document.getNationality(),
            document.getIdNumber(),
            document.getAddress(),
            document.getCity(),
            document.getPostalCode(),
            document.getCountry(),
            document.getIdFrontPhotoPath(),
            document.getIdBackPhotoPath(),
            document.getSubmittedAt(),
            user.getKycStatus()

            // Review fields removed - now stored in User entity
        );
    }
}