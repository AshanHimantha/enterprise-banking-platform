package auth.service;

import dto.EmployeeCreateDTO;
import dto.UserDTO;
import dto.EmployeeDTO;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.ejb.Local;
import java.util.List;
import java.util.Optional;

@Local
public interface UserManagementService {

    List<UserDTO> getAllUsers();

    Optional<UserDTO> findUserByUsername(String username);

    void suspendUser(String usernameToSuspend, String adminUsername, String reason);

    void reactivateUser(String usernameToReactivate, String adminUsername);

    List<UserDTO> searchUsers(int page, int limit, AccountLevel accountLevel,
                             UserStatus status, KycStatus kycStatus,
                             String username, String email);

    int countUsers(AccountLevel accountLevel, UserStatus status, KycStatus kycStatus,
                   String username, String email);

    EmployeeDTO createEmployee(EmployeeCreateDTO dto);
    List<EmployeeDTO> getAllEmployees();
    EmployeeDTO addRoleToUser(String username, String roleToAdd);
    EmployeeDTO removeRoleFromUser(String username, String roleToRemove);
}