package service;

import util.AuditingInterceptor;
import dto.CreateVirtualCardDTO;
import dto.UnmaskedVirtualCardDTO;
import dto.VirtualCardDTO;
import entity.Account;
import entity.User;
import entity.VirtualCard;
import enums.AccountType;
import enums.VirtualCardStatus;
import exception.BusinessRuleException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import util.LoggingInterceptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
@RolesAllowed("CUSTOMER")
@Interceptors({LoggingInterceptor.class, AuditingInterceptor.class})
public class VirtualCardServiceImpl implements VirtualCardService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    @EJB
    private CardDetailsGenerator cardDetailsGenerator;

    @Override
    public VirtualCardDTO createVirtualCard(String username, CreateVirtualCardDTO dto) {
        Account linkedAccount = findAccountByNumber(dto.getFromAccountNumber());
        User user = findUserByUsername(username);

        // Security Check: User must own the account
        if (linkedAccount.getOwner() == null || !linkedAccount.getOwner().equals(user)) {
            throw new SecurityException("Authorization error: You do not own the source account.");
        }

        // Business Rule 1: Account Type must be SAVING or CURRENT
        if (linkedAccount.getAccountType() != AccountType.SAVING && linkedAccount.getAccountType() != AccountType.CURRENT) {
            throw new IllegalArgumentException("Virtual cards can only be linked to SAVING or CURRENT accounts.");
        }

   TypedQuery<Long> query = em.createQuery(
           "SELECT COUNT(vc) FROM VirtualCard vc WHERE vc.linkedAccount = :account AND vc.status <> :terminatedStatus", Long.class);
   query.setParameter("account", linkedAccount);
   query.setParameter("terminatedStatus", VirtualCardStatus.TERMINATED);

        long existingCardCount = query.getSingleResult();
        final long MAX_CARDS_PER_ACCOUNT = 2;

        if (existingCardCount >= MAX_CARDS_PER_ACCOUNT) {

            throw new BusinessRuleException("Limit reached: You cannot create more than " +
                    MAX_CARDS_PER_ACCOUNT + " virtual cards for this account.");
        }

        // All checks passed, create the card
        VirtualCard newCard = new VirtualCard();
        newCard.setLinkedAccount(linkedAccount);
        newCard.setCardHolderName(user.getFirstName().toUpperCase() + " " + user.getLastName().toUpperCase());
        newCard.setNickname(dto.getNickname());
        newCard.setSpendingLimit(dto.getSpendingLimit());
        newCard.setStatus(VirtualCardStatus.ACTIVE);
        newCard.setCreatedAt(LocalDateTime.now());

        // Generate new card details using a dedicated utility
        newCard.setCardNumber(cardDetailsGenerator.generateNewCardNumber());
        newCard.setExpiryDate(cardDetailsGenerator.generateExpiryDate());
        newCard.setCvv(cardDetailsGenerator.generateCvv());

        em.persist(newCard);
        return new VirtualCardDTO(newCard);
    }

@Override
public List<VirtualCardDTO> getVirtualCardsForUser(String username) {
    return em.createQuery(
            "SELECT vc FROM VirtualCard vc WHERE vc.linkedAccount.owner.username = :username AND vc.status <> :terminatedStatus ORDER BY vc.createdAt DESC",
            VirtualCard.class)
            .setParameter("username", username)
            .setParameter("terminatedStatus", VirtualCardStatus.TERMINATED)
            .getResultStream()
            .map(VirtualCardDTO::new)
            .collect(Collectors.toList());
}

    @Override
    public VirtualCardDTO freezeVirtualCard(String username, Long cardId) {
        VirtualCard card = findUserCardById(username, cardId);
        if (card.getStatus() != VirtualCardStatus.ACTIVE) {
            throw new IllegalStateException("Only an ACTIVE card can be frozen.");
        }
        card.setStatus(VirtualCardStatus.FROZEN);
        em.merge(card);
        return new VirtualCardDTO(card);
    }

    @Override
    public VirtualCardDTO unfreezeVirtualCard(String username, Long cardId) {
        VirtualCard card = findUserCardById(username, cardId);
        if (card.getStatus() != VirtualCardStatus.FROZEN) {
            throw new IllegalStateException("Only a FROZEN card can be unfrozen.");
        }
        card.setStatus(VirtualCardStatus.ACTIVE);
        em.merge(card);
        return new VirtualCardDTO(card);
    }

    @Override
    public void terminateVirtualCard(String username, Long cardId) {
        VirtualCard card = findUserCardById(username, cardId);
        if (card.getStatus() == VirtualCardStatus.TERMINATED) {
            return; // Already terminated, nothing to do
        }
        card.setStatus(VirtualCardStatus.TERMINATED);
        em.merge(card);
    }

    @Override
    public VirtualCardDTO updateSpendingLimit(String username, Long cardId, BigDecimal newLimit) {
        VirtualCard card = findUserCardById(username, cardId);
        if (newLimit != null && newLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Spending limit cannot be negative.");
        }
        card.setSpendingLimit(newLimit);
        em.merge(card);
        return new VirtualCardDTO(card);
    }

    @Override
    public Optional<UnmaskedVirtualCardDTO> getUnmaskedCardDetails(String username, Long cardId, String userPassword) {
        // 1. Find the card and ensure the user owns it.
        VirtualCard card = findUserCardById(username, cardId);

        // 2. Re-authenticate the user by checking their main password.
        User user = card.getLinkedAccount().getOwner();
        if (!user.getPassword().equals(hashPassword(userPassword))) {
            throw new SecurityException("Incorrect password. Cannot reveal card details.");
        }

        // 3. Check if the card is in a state where details should be revealed.
        if (card.getStatus() == VirtualCardStatus.TERMINATED) {
            throw new IllegalStateException("Cannot reveal details for a terminated card.");
        }

        // 4. If all checks pass, create and return the DTO with the unmasked data.
        return Optional.of(new UnmaskedVirtualCardDTO(card));
    }

    @Override
    public void setOrChangePin(String username, Long cardId, String userPassword, String newPin) {
        VirtualCard card = findUserCardById(username, cardId);
        User user = card.getLinkedAccount().getOwner();

        // Authorize the action by verifying the user's main login password
        if (!user.getPassword().equals(hashPassword(userPassword))) {
            throw new SecurityException("Incorrect password. PIN change not authorized.");
        }

        // Validate the new PIN
        if (newPin == null || !newPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }

        // Hash and set the new PIN
        card.setHashedPin(hashPassword(newPin));
        em.merge(card);
    }

    @Override
    public VirtualCardDTO createVirtualCard(String username, String fromAccountNumber, String nickname) {
        return null;
    }

    // --- Helper Methods ---

    private Account findAccountByNumber(String accountNumber) {
        try {
            return em.createQuery("SELECT a FROM Account a JOIN FETCH a.owner WHERE a.accountNumber = :accountNumber", Account.class)
                    .setParameter("accountNumber", accountNumber)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("Account with number " + accountNumber + " not found.");
        }
    }

    private User findUserByUsername(String username) {
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username).getSingleResult();
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User with username " + username + " not found.");
        }
    }

    private VirtualCard findUserCardById(String username, Long cardId) {
        try {
            return em.createQuery("SELECT vc FROM VirtualCard vc WHERE vc.id = :cardId AND vc.linkedAccount.owner.username = :username", VirtualCard.class)
                    .setParameter("cardId", cardId)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            throw new SecurityException("Virtual card not found or you do not have permission to access it.");
        }
    }

    // This should be extracted to a shared PasswordHashing utility bean for best practice.
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}