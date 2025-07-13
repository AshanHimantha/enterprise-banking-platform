package auth.service;

import dto.UserDTO;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.ejb.Local;
import java.util.List;
import java.util.Optional;

@Local
public interface UserManagementService {
    /** Returns a list of all users in the system. */
    List<UserDTO> getAllUsers();

    /** Finds a single user by their username. */
    Optional<UserDTO> findUserByUsername(String username);

    /** Suspends a user's account for a given reason. */
    void suspendUser(String usernameToSuspend, String adminUsername, String reason);

    /** Reactivates a previously suspended user's account. */
    void reactivateUser(String usernameToReactivate, String adminUsername);

    /**
     * Searches for users with pagination and filtering support.
     * @param page Page number (1-based)
     * @param limit Number of results per page
     * @param accountLevel Filter by account level (optional)
     * @param status Filter by user status (optional)
     * @param kycStatus Filter by KYC status (optional)
     * @param username Search by username (partial match, optional)
     * @param email Search by email (partial match, optional)
     * @return List of matching users
     */
    List<UserDTO> searchUsers(int page, int limit, AccountLevel accountLevel,
                             UserStatus status, KycStatus kycStatus,
                             String username, String email);

    /**
     * Counts total number of users matching the given criteria.
     * @param accountLevel Filter by account level (optional)
     * @param status Filter by user status (optional)
     * @param kycStatus Filter by KYC status (optional)
     * @param username Search by username (partial match, optional)
     * @param email Search by email (partial match, optional)
     * @return Total count of matching users
     */
    int countUsers(AccountLevel accountLevel, UserStatus status, KycStatus kycStatus,
                   String username, String email);
}