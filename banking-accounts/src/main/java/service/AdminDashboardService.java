package service;

import dto.AdminDashboardDTO;
import jakarta.ejb.Local;

@Local
public interface AdminDashboardService {
    AdminDashboardDTO getDashboardSummary();
}
