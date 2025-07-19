package auth.service;

import dto.UserAuditDTO;
import jakarta.ejb.Local;

@Local
public interface UserAuditService {
    UserAuditDTO getFullUserAudit(String username, int pageNumber, int pageSize);

}
