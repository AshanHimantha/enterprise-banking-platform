package rest;

import dto.KycDocumentDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
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
import java.util.HashMap;
import java.util.Map;

@Path("/kyc")
public class KycController {

    @EJB
    private KycService kycService;

    @POST
    @Path("/submit")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("NONE") // The user must be logged in, even without a full role yet
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
            // Validate required fields
            if (fullName == null || fullName.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Full name is required");
            }

            if (dateOfBirthStr == null || dateOfBirthStr.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Date of birth is required");
            }

            if (nationality == null || nationality.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Nationality is required");
            }

            if (idNumber == null || idNumber.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "ID number is required");
            }

            if (idFrontPhotoStream == null || idBackPhotoStream == null) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Both front and back photos of ID are required");
            }

            String username = securityContext.getUserPrincipal().getName();
            LocalDate dateOfBirth = LocalDate.parse(dateOfBirthStr); // Assumes "YYYY-MM-DD" format

            kycService.submitKyc(
                    username, fullName, dateOfBirth, nationality, idNumber, address, city, postalCode, country,
                    idFrontPhotoStream, idFrontPhotoDetails.getFileName(),
                    idBackPhotoStream, idBackPhotoDetails.getFileName()
            );

            return createSuccessResponse("KYC documents submitted successfully. Awaiting review.");

        } catch (DateTimeParseException e) {
            return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid date format. Please use 'YYYY-MM-DD'.");
        } catch (IllegalStateException e) {
            return createErrorResponse(Response.Status.CONFLICT, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "KYC submission failed: " + e.getMessage());
        }
    }

    /**
     * Get KYC document by username (Admin/Employee only, or own document)
     */
    @GET
    @Path("/documents/user/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE", "CUSTOMER","NONE"})
    public Response getKycDocumentByUsername(@Context SecurityContext securityContext, @PathParam("username") String username) {
        try {
            String currentUser = securityContext.getUserPrincipal().getName();
            boolean isAdmin = securityContext.isUserInRole("ADMIN") || securityContext.isUserInRole("EMPLOYEE");

            // Users can only view their own KYC documents unless they are admin/employee
            if (!isAdmin && !currentUser.equals(username)) {
                return createErrorResponse(Response.Status.FORBIDDEN, "You can only view your own KYC documents");
            }

            KycDocumentDto document = kycService.getKycDocumentByUsername(username);
            if (document == null) {
                return createErrorResponse(Response.Status.NOT_FOUND, "No KYC document found for user: " + username);
            }

            return createDataResponse(document, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC document: " + e.getMessage());
        }
    }

    /**
     * Get current user's KYC status and document
     */
    @GET
    @Path("/my-status")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"CUSTOMER", "ADMIN", "EMPLOYEE"})
    public Response getMyKycStatus(@Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            KycDocumentDto document = kycService.getKycDocumentByUsername(username);

            if (document == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasKyc", false);
                response.put("message", "No KYC document found. Please submit your KYC documents.");
                response.put("timestamp", System.currentTimeMillis());
                return Response.ok(response).build();
            }

            return createDataResponse(document, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC status: " + e.getMessage());
        }
    }

    // Helper methods
    private Response createSuccessResponse(String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", message);
        responseBody.put("timestamp", System.currentTimeMillis());
        return Response.ok(responseBody).build();
    }

    private Response createErrorResponse(Response.Status status, String errorMessage) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("error", errorMessage);
        responseBody.put("timestamp", System.currentTimeMillis());
        return Response.status(status).entity(responseBody).build();
    }

    private Response createDataResponse(Object data, long count) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", data);
        responseBody.put("count", count);
        responseBody.put("timestamp", System.currentTimeMillis());
        return Response.ok(responseBody).build();
    }
}
