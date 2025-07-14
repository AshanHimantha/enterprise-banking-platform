package service;


import dto.BillerDTO;

import enums.BillerCategory;
import enums.BillerStatus;
import jakarta.ejb.Local;

import java.io.InputStream;
import java.util.List;

@Local
public interface BilllerService {
    BillerDTO createBiller(String billerName, BillerCategory category, InputStream logoStream, String fileName);
    List<BillerDTO> getAllBillers();
    void updateBillerStatus(Long billerId, BillerStatus newStatus);
}