package service.impl;


import dto.BillerDTO;
import entity.Account;
import entity.Biller;
import enums.AccountType;
import enums.BillerCategory;
import enums.BillerStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import service.AccountService;
import service.BilllerService;
import util.LoggingInterceptor;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Stateless
@Interceptors(LoggingInterceptor.class)
public class BillerServiceImpl implements BilllerService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private AccountService accountNumberGenerator;

    @RolesAllowed("ADMIN")
    @Override
    public BillerDTO createBiller(String billerName, BillerCategory category, InputStream logoStream, String fileName) {
        if (em.createQuery("SELECT COUNT(b) FROM Biller b WHERE b.billerName = :name", Long.class)
                .setParameter("name", billerName).getSingleResult() > 0) {
            throw new IllegalStateException("A biller with the name '" + billerName + "' already exists.");
        }
        Biller biller = new Biller();
        biller.setBillerName(billerName);
        biller.setCategory(category);
        biller.setStatus(BillerStatus.ACTIVE);

        if (logoStream != null && fileName != null) {
            try {
                String savedLogoPath = saveLogoFile(logoStream, fileName, billerName);
                String logoFilename = Paths.get(savedLogoPath).getFileName().toString();
                biller.setLogoUrl(logoFilename);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save biller logo file.", e);
            }
        }

        Account billerAccount = new Account();
        billerAccount.setAccountNumber(accountNumberGenerator.generateHumanReadableAccountNumber());
        billerAccount.setBalance(BigDecimal.ZERO);
        billerAccount.setAccountType(AccountType.BILLER);
        billerAccount.setOwner(null);

        biller.setInternalAccount(billerAccount);
        em.persist(biller);

        return new BillerDTO(biller);
    }
    @RolesAllowed({"ADMIN", "EMPLOYEE", "CUSTOMER"})
    @Override
    public List<BillerDTO> getAllBillers() {
        return em.createQuery("SELECT b FROM Biller b ORDER BY b.billerName", Biller.class)
                .getResultStream()
                .map(BillerDTO::new)
                .collect(Collectors.toList());
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    @Override
    public void updateBillerStatus(Long billerId, BillerStatus newStatus) {
        Biller biller = em.find(Biller.class, billerId);
        if (biller == null) {
            throw new IllegalArgumentException("Biller with ID " + billerId + " not found.");
        }
        biller.setStatus(newStatus);
        em.merge(biller);
    }

    // --- Helper Methods for File Handling ---
    private String getBillerLogoDirectory() {
        return "C:\\banking_uploads\\biller-logos\\";
    }

    private String saveLogoFile(InputStream inputStream, String originalFileName, String billerName) throws Exception {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String sanitizedBillerName = billerName.replaceAll("[^a-zA-Z0-9.-]", "_");
        String uniqueFileName = sanitizedBillerName + "_logo_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        File uploadDir = new File(getBillerLogoDirectory());
        if (!uploadDir.exists()) uploadDir.mkdirs();
        Path destination = Paths.get(getBillerLogoDirectory() + uniqueFileName);
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toString();
    }
}