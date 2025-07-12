package rest;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import service.KycService;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;


@Path("/kyc")
public class KycController {

    @EJB
    private KycService kycService;

    @POST
    @Path("/submit")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("CUSTOMER") // The user must be logged in, even without a full role yet
    public Response submitKyc(
            @Context SecurityContext securityContext,
            @FormDataParam("fullName") String fullName,
            @FormDataParam("dateOfBirth") String dateOfBirthStr,
            @FormDataParam("nationality") String nationality,
            @FormDataParam("idNumber") String idNumber,
            @FormDataParam("address") String address,
            @FormDataParam("city") String city,
            @FormDataParam("postalCode") String postalCode,
            @FormDataParam("country") String country,
            @FormDataParam("idFrontPhoto") InputStream idFrontPhotoStream,
            @FormDataParam("idFrontPhoto") FormDataContentDisposition idFrontPhotoDetails,
            @FormDataParam("idBackPhoto") InputStream idBackPhotoStream,
            @FormDataParam("idBackPhoto") FormDataContentDisposition idBackPhotoDetails) {

        try {
            String username = securityContext.getUserPrincipal().getName();
            LocalDate dateOfBirth = LocalDate.parse(dateOfBirthStr); // Assumes "YYYY-MM-DD" format

            kycService.submitKyc(
                    username, fullName, dateOfBirth, nationality, idNumber, address, city, postalCode, country,
                    idFrontPhotoStream, idFrontPhotoDetails.getFileName(),
                    idBackPhotoStream, idBackPhotoDetails.getFileName()
            );

            return Response.ok(Collections.singletonMap("message", "KYC documents submitted successfully. Awaiting review.")).build();
        } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid date format. Please use 'YYYY-MM-DD'."))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "KYC submission failed: " + e.getMessage()))
                    .build();
        }
    }
}