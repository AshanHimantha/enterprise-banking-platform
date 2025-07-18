package service;

import jakarta.ejb.Singleton;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Random;

@Singleton
public class CardDetailsGenerator {
    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    public synchronized String generateNewCardNumber() {
        Random random = new Random();
        String cardNumber;
        do {
            StringBuilder builder = new StringBuilder("4"); // Visa
            for (int i = 0; i < 15; i++) {
                builder.append(random.nextInt(10));
            }
            cardNumber = builder.toString();
        } while (cardNumberExists(cardNumber));
        return cardNumber;
    }

    public String generateCvv() {
        return String.format("%03d", new Random().nextInt(1000));
    }

    public LocalDate generateExpiryDate() {
        return LocalDate.now().plusYears(5);
    }

    private boolean cardNumberExists(String cardNumber) {
        return em.createQuery("SELECT COUNT(vc) FROM VirtualCard vc WHERE vc.cardNumber = :number", Long.class)
                .setParameter("number", cardNumber)
                .getSingleResult() > 0;
    }
}