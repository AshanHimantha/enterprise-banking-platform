package rest;


import auth.service.AdminService;
import dto.KycDocumentDto;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.KycService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/admin")
public class AdminKYCController {

    @EJB
    private AdminService adminService;

    @EJB
    private KycService kycService;

    @POST
    @Path("/users/{username}/approve-kyc")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Secure the API endpoint
    public Response approveKyc(@PathParam("username") String username, Map<String, String> request) {
        try {
            String reviewNotes = request.get("reviewNotes");
            String reviewedBy = request.get("reviewedBy");

            // Default values if not provided
            if (reviewNotes == null) {
                reviewNotes = "";
            }
            if (reviewedBy == null) {
                reviewedBy = "SYSTEM";
            }

            adminService.approveKycAndAssignRole(username, reviewNotes, reviewedBy);
            return Response.ok(Collections.singletonMap("message", "KYC approved and role assigned for user " + username)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", "User not found or operation failed: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/users/{username}/reject-kyc")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Secure the API endpoint
    public Response rejectKyc(@PathParam("username") String username, Map<String, String> request) {
        try {
            String reviewNotes = request.get("reviewNotes");
            String reviewedBy = request.get("reviewedBy");

            // Default values if not provided
            if (reviewNotes == null) {
                reviewNotes = "";
            }
            if (reviewedBy == null) {
                reviewedBy = "SYSTEM";
            }

            adminService.rejectKyc(username, reviewNotes, reviewedBy);
            return Response.ok(Collections.singletonMap("message", "KYC rejected for user " + username)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", "User not found or operation failed: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Get all KYC documents (Admin only)
     */
    @GET
    @Path("/kyc/documents")
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
    @Path("/kyc/documents/status/{status}")
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
     * Get KYC document by ID (Admin/Employee only)
     */
    @GET
    @Path("/kyc/documents/{id}")
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
    @Path("/kyc/documents/paginated")
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
     * Get KYC image file (Admin only)
     * This endpoint serves KYC images with proper security
     */
    @GET
    @Path("/kyc/images/{filename}")
    @RolesAllowed({"ADMIN", "EMPLOYEE"})
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
                kycDir = webappPath + "/applications/banking-ear/assets/kyc/";
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
     * Get KYC images for a specific document (Admin/Employee only)
     * Returns URLs to access the front and back ID images
     */
    @GET
    @Path("/kyc/documents/{id}/images")
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
            imageUrls.put("frontImageUrl", "/api/admin/kyc/images/" + frontPhotoPath);
            imageUrls.put("backImageUrl", "/api/admin/kyc/images/" + backPhotoPath);
            imageUrls.put("documentId", id);
            imageUrls.put("username", document.getUsername());

            return createDataResponse(imageUrls, 1);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve image URLs: " + e.getMessage());
        }
    }

    /**
     * Get file system info endpoint to show webapp directory (Admin only)
     */
    @GET
    @Path("/kyc/files/info")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"ADMIN"})
    public Response getFileSystemInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            // Get webapp KYC directory info
            String webappPath = System.getProperty("com.sun.aas.instanceRoot");
            String kycDir;
            if (webappPath != null) {
                kycDir = webappPath + "/applications/banking-ear/assets/kyc/";
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
                        fileInfo.put("webUrl", "/bank/api/admin/kyc/images/" + file.getName());
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
        // Get the actual stored path from the database
        try {
            KycDocumentDto document = kycService.getKycDocumentById(documentId);
            if (document == null) {
                return null;
            }

            if ("front".equals(type)) {
                return document.getIdFrontPhotoPath();
            } else if ("back".equals(type)) {
                return document.getIdBackPhotoPath();
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
