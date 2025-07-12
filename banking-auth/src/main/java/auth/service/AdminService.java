package auth.service;


import jakarta.ejb.Local;

@Local
public interface AdminService {
    void approveKycAndAssignRole(String username);
}