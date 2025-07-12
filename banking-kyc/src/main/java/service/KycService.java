package service;


import jakarta.ejb.Local;

import java.io.InputStream;
import java.time.LocalDate;

@Local
public interface KycService {

    void submitKyc(
            String username,
            String fullName,
            LocalDate dateOfBirth,
            String nationality,
            String idNumber,
            String address,
            String city,
            String postalCode,
            String country,
            InputStream idFrontPhotoStream,
            String idFrontPhotoFileName,
            InputStream idBackPhotoStream,
            String idBackPhotoFileName
    );

}