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

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * Get all KYC documents (Admin only)
     */
    @GET
    @Path("/documents")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response getAllKycDocuments() {
        try {
            List<KycDocumentDto> documents = kycService.getAllKycDocuments();
            return createDataResponse(documents, documents.size());
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC documents: " + e.getMessage());
        }
    }

    /**
     * Get KYC documents by status (Admin/Employee only)
     */
    @GET
    @Path("/documents/status/{status}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Response getKycDocumentsByStatus(@PathParam("status") String status) {
        try {
            List<KycDocumentDto> documents = kycService.getKycDocumentsByStatus(status);
            return createDataResponse(documents, documents.size());
        } catch (IllegalArgumentException e) {
            return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid status: " + status + ". Valid statuses are: PENDING, VERIFIED, REJECTED");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC documents: " + e.getMessage());
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
     * Get KYC document by ID (Admin/Employee only)
     */
    @GET
    @Path("/documents/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Response getKycDocumentById(@PathParam("id") Long id) {
        try {
            KycDocumentDto document = kycService.getKycDocumentById(id);
            if (document == null) {
                return createErrorResponse(Response.Status.NOT_FOUND, "No KYC document found with ID: " + id);
            }

            return createDataResponse(document, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve KYC document: " + e.getMessage());
        }
    }

    /**
     * Get paginated KYC documents (Admin/Employee only)
     */
    @GET
    @Path("/documents/paginated")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Response getKycDocumentsPaginated(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        try {
            if (page < 0) page = 0;
            if (size < 1 || size > 100) size = 10; // Limit page size to prevent performance issues

            List<KycDocumentDto> documents = kycService.getKycDocumentsPaginated(page, size);
            long totalCount = kycService.getKycDocumentsCount();

            return createPaginatedResponse(documents, page, size, totalCount);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve paginated KYC documents: " + e.getMessage());
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

    /**
     * Get KYC image file (Admin only)
     * This endpoint serves KYC images with proper security
     */
    @GET
    @Path("/images/{filename}")
    public Response getKycImage(@PathParam("filename") String filename) {
        try {
            // Validate filename to prevent directory traversal attacks
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid filename");
            }

            // Get the webapp's KYC directory path
            String webappPath = System.getProperty("com.sun.aas.instanceRoot");
            String kycDir;
            if (webappPath != null) {
                kycDir = webappPath + "/applications/banking-ear/com.ashanhimantha.ee-banking-web-1.0_war/assets/kyc/";
            } else {
                kycDir = "C:\\banking_uploads\\kyc_images\\";
            }

            File imageFile = new File(kycDir + filename);

            if (!imageFile.exists()) {
                return createErrorResponse(Response.Status.NOT_FOUND, "Image not found");
            }

            // Determine content type based on file extension
            String contentType = "image/jpeg"; // default
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            switch (extension) {
                case "png":
                    contentType = "image/png";
                    break;
                case "jpg":
                case "jpeg":
                    contentType = "image/jpeg";
                    break;
                case "gif":
                    contentType = "image/gif";
                    break;
                default:
                    return createErrorResponse(Response.Status.BAD_REQUEST, "Unsupported image format");
            }

            return Response.ok(imageFile)
                    .type(contentType)
                    .header("Content-Disposition", "inline; filename=\"" + filename + "\"")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve image: " + e.getMessage());
        }
    }

    /**
     * Get KYC images for a specific document (Admin only)
     * Returns URLs to access the front and back ID images
     */
    @GET
    @Path("/documents/{id}/images")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    public Response getKycDocumentImages(@PathParam("id") Long id) {
        try {
            KycDocumentDto document = kycService.getKycDocumentById(id);
            if (document == null) {
                return createErrorResponse(Response.Status.NOT_FOUND, "No KYC document found with ID: " + id);
            }

            // Extract filenames from the stored paths
            String frontPhotoPath = extractFilename(getStoredPath(document.getId(), "front"));
            String backPhotoPath = extractFilename(getStoredPath(document.getId(), "back"));

            Map<String, Object> imageUrls = new HashMap<>();
            imageUrls.put("frontImageUrl", "/bank/api/kyc/images/" + frontPhotoPath);
            imageUrls.put("backImageUrl", "/bank/api/kyc/images/" + backPhotoPath);
            imageUrls.put("documentId", id);
            imageUrls.put("username", document.getUsername());

            return createDataResponse(imageUrls, 1);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve image URLs: " + e.getMessage());
        }
    }

    /**
     * Update file system info endpoint to show webapp directory
     */
    @GET
    @Path("/files/info")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN"})
    public Response getFileSystemInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // Get webapp KYC directory info
            String webappPath = System.getProperty("com.sun.aas.instanceRoot");
            String kycDir;
            if (webappPath != null) {
                kycDir = webappPath + "/applications/banking-ear/com.ashanhimantha.ee-banking-web-1.0_war/assets/kyc/";
            } else {
                kycDir = "C:\\banking_uploads\\kyc_images\\";
            }

            File directory = new File(kycDir);

            info.put("uploadDirectory", kycDir);
            info.put("directoryExists", directory.exists());
            info.put("directoryCanWrite", directory.canWrite());
            info.put("directoryCanRead", directory.canRead());
            info.put("isWebappDirectory", webappPath != null);

            if (directory.exists()) {
                File[] files = directory.listFiles();
                info.put("totalFiles", files != null ? files.length : 0);

                if (files != null && files.length > 0) {
                    List<Map<String, Object>> fileList = new ArrayList<>();
                    for (File file : files) {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", file.getName());
                        fileInfo.put("size", file.length());
                        fileInfo.put("lastModified", file.lastModified());
                        fileInfo.put("path", file.getAbsolutePath());
                        fileInfo.put("webUrl", "/bank/api/kyc/images/" + file.getName());
                        fileList.add(fileInfo);
                    }
                    info.put("files", fileList);
                }
            }

            return createDataResponse(info, 1);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to get file system info: " + e.getMessage());
        }
    }

    // Helper methods
    private String extractFilename(String fullPath) {
        if (fullPath == null) return null;
        return fullPath.substring(fullPath.lastIndexOf(File.separator) + 1);
    }

    private String getStoredPath(Long documentId, String type) {
        // This would need to be implemented to get the actual stored path from the database
        // For now, return a placeholder
        return "placeholder_" + documentId + "_" + type + ".jpg";
    }

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

    private Response createPaginatedResponse(List<KycDocumentDto> data, int page, int size, long totalCount) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", data);
        responseBody.put("pagination", createPaginationInfo(page, size, totalCount));
        responseBody.put("timestamp", System.currentTimeMillis());
        return Response.ok(responseBody).build();
    }

    private Map<String, Object> createPaginationInfo(int page, int size, long totalCount) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalCount", totalCount);
        pagination.put("totalPages", (int) Math.ceil((double) totalCount / size));
        pagination.put("hasNext", (page + 1) * size < totalCount);
        pagination.put("hasPrevious", page > 0);
        return pagination;
    }
}

