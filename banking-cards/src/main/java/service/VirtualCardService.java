package service;




import dto.CreateVirtualCardDTO;
import dto.UnmaskedVirtualCardDTO;
import dto.VirtualCardDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Local
public interface VirtualCardService {


    VirtualCardDTO createVirtualCard(String username, CreateVirtualCardDTO dto);

    List<VirtualCardDTO> getVirtualCardsForUser(String username);

    VirtualCardDTO freezeVirtualCard(String username, Long cardId);

    VirtualCardDTO unfreezeVirtualCard(String username, Long cardId);


    void terminateVirtualCard(String username, Long cardId);


    VirtualCardDTO updateSpendingLimit(String username, Long cardId, BigDecimal newLimit);
    void setOrChangePin(String username, Long cardId, String userPassword, String newPin);

    @RolesAllowed("CUSTOMER")
    VirtualCardDTO createVirtualCard(String username, String fromAccountNumber, String nickname);
    Optional<UnmaskedVirtualCardDTO> getUnmaskedCardDetails(String username, Long cardId, String userPassword);
}