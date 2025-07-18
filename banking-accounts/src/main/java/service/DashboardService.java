package service;

import dto.DashboardSummaryDTO;
import jakarta.ejb.Local;

@Local
public interface DashboardService {

    DashboardSummaryDTO getDashboardSummary(String username);

}