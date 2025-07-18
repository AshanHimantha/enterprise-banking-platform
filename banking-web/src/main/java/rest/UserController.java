package rest;


import auth.service.UserService;
import dto.ProfileUpdateDTO;
import entity.User;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Path("/user/profile")
public class UserController {

    @EJB
    private UserService userService;

    // We use PUT for updates as it's idempotent for updating a whole resource representation,
    // but PATCH is also a good choice if you want to allow partial updates.
    @PUT
    @Path("/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"}) // Any authenticated user can update their own profile
    public Response updateProfile(ProfileUpdateDTO profileUpdateDTO, @Context SecurityContext securityContext) {

        // Get the username from the JWT token to ensure users can only update their own profile.
        String username = securityContext.getUserPrincipal().getName();

        try {
            userService.updateUserProfile(username, profileUpdateDTO);
            return Response.ok(Collections.singletonMap("message", "Profile updated successfully.")).build();
        } catch (Exception e) {
            // This could catch validation errors or if the user is not found.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Failed to update profile: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Upload profile picture
     */
    @POST
    @Path("/avatar")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"})
    public Response uploadProfilePicture(
            @Context SecurityContext securityContext,
            @FormDataParam("avatar") InputStream avatarStream,
            @FormDataParam("avatar") FormDataContentDisposition avatarDetails) {

        try {
            // Validate required fields
            if (avatarStream == null) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Avatar image is required");
            }

            if (avatarDetails == null || avatarDetails.getFileName() == null) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Avatar filename is required");
            }

            // Validate file type
            String fileName = avatarDetails.getFileName().toLowerCase();
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Only JPG, JPEG, and PNG files are allowed");
            }

            String username = securityContext.getUserPrincipal().getName();

            // Call service to handle avatar upload
            String avatarUrl = userService.uploadProfilePicture(username, avatarStream, avatarDetails.getFileName());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile picture uploaded successfully");
            response.put("avatarUrl", avatarUrl);
            response.put("timestamp", System.currentTimeMillis());

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return createErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to upload profile picture: " + e.getMessage());
        }
    }

    /**
     * Get current user's profile picture
     */
    @GET
    @Path("/avatar")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"})
    public Response getProfilePicture(@Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            String avatarUrl = userService.getProfilePictureUrl(username);

            if (avatarUrl == null) {
                return createErrorResponse(Response.Status.NOT_FOUND, "No profile picture found");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("avatarUrl", avatarUrl);
            response.put("timestamp", System.currentTimeMillis());

            return Response.ok(response).build();

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve profile picture: " + e.getMessage());
        }
    }


    @GET
    @Path("/myprofile")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"CUSTOMER", "EMPLOYEE", "ADMIN"})
    public Response getMyProfile(@Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            User user = userService.getUserProfile(username);

            if (user == null) {
                return createErrorResponse(Response.Status.NOT_FOUND, "User profile not found");
            }

            Map<String, Object> profileData = new HashMap<>();
            profileData.put("id", user.getId());
            profileData.put("username", user.getUsername());
            profileData.put("firstName", user.getFirstName());
            profileData.put("middleName", user.getMiddleName());
            profileData.put("lastName", user.getLastName());
            profileData.put("email", user.getEmail());
            profileData.put("phoneNumber", user.getPhoneNumber());
            profileData.put("address", user.getAddress());
            profileData.put("emailVerified", user.isEmailVerified());
            profileData.put("kycStatus", user.getKycStatus());
            profileData.put("accountLevel", user.getAccountLevel());
            profileData.put("status", user.getStatus());
            profileData.put("registeredDate", user.getRegisteredDate());
            profileData.put("lastLoginDate", user.getLastLoginDate());
            profileData.put("giftClaimed", user.isHasClaimedWelcomeGift());

            // Only store filename in DB, so check for empty or null
            String avatarFilename = user.getProfilePictureUrl();
            System.out.println("Avatar URL: " + avatarFilename);

            if (avatarFilename != null && !avatarFilename.trim().isEmpty()) {
                profileData.put("avatarUrl", "/api/user/profile/avatar/image/" + avatarFilename);
                profileData.put("hasAvatar", true);
            } else {
                profileData.put("avatarUrl", null);
                profileData.put("hasAvatar", false);
            }

            return createDataResponse(profileData, 1);

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve user profile: " + e.getMessage());
        }
    }

    /**
     * Get avatar image file - Public access (no authentication required)
     * This endpoint serves avatar images publicly without JWT authentication
     */
    @GET
    @Path("/avatar/image/{filename}")
    public Response getAvatarImage(@PathParam("filename") String filename) {
        try {
            // Validate filename to prevent directory traversal attacks
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return createErrorResponse(Response.Status.BAD_REQUEST, "Invalid filename");
            }

            // Get the webapp's avatar directory path
            String webappPath = System.getProperty("com.sun.aas.instanceRoot");
            String avatarDir;
            if (webappPath != null) {
                avatarDir = webappPath + "/applications/banking-ear/assets/avatars/";
            } else {
                avatarDir = "C:\\banking_uploads\\avatars\\";
            }

            File imageFile = new File(avatarDir + filename);

            if (!imageFile.exists()) {
                return createErrorResponse(Response.Status.NOT_FOUND, "Avatar image not found");
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
                    .header("Cache-Control", "public, max-age=3600") // Cache for 1 hour
                    .header("Access-Control-Allow-Origin", "*") // Allow cross-origin requests
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve avatar image: " + e.getMessage());
        }
    }




    // Helper methods
    private String extractFilename(String fullPath) {
        if (fullPath == null) return null;
        return fullPath.substring(fullPath.lastIndexOf("/") + 1);
    }

    private Response createDataResponse(Object data, long count) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("data", data);
        responseBody.put("count", count);
        responseBody.put("timestamp", System.currentTimeMillis());
        return Response.ok(responseBody).build();
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
}
