package service;

import jakarta.ejb.Local;
import java.math.BigDecimal;

@Local
public interface GiftService {

    BigDecimal claimWelcomeGift(String username);
}