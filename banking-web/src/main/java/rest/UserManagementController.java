package rest;

import auth.service.UserAuditService;
import auth.service.UserManagementService;
import dto.UserAuditDTO;
import dto.UserDTO;
import enums.AccountLevel;
import enums.KycStatus;
import enums.UserStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;

@Path("/admin/manage/users")
public class UserManagementController {

    @EJB
    private UserManagementService userManagementService;

    @EJB
    private UserAuditService userAuditService;

    private static final Logger logger = Logger.getLogger(UserManagementController.class.getName());

    public static class SuspendRequestDTO {
        public String reason;
    }

    public static class UserSearchResponse {
        public List<UserDTO> users;
        public int totalCount;
        public int page;
        public int limit;
        public boolean hasMore;

        public UserSearchResponse(List<UserDTO> users, int totalCount, int page, int limit) {
            this.users = users;
            this.totalCount = totalCount;
            this.page = page;
            this.limit = limit;
            this.hasMore = (page * limit) < totalCount;
        }
    }

    public static class ErrorResponse {
        public String error;
        public String message;
        public String timestamp;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
            this.timestamp = java.time.LocalDateTime.now().toString();
        }

        @Override
        public String toString() {
            return message;
        }
    }

    private Response createErrorResponse(Response.Status status, String errorCode, String message) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        return Response.status(status)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON)
                .header("X-Error-Message", message)
                .build();
    }


    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllUsers() {
        try {
            List<UserDTO> users = userManagementService.getAllUsers();
            return Response.ok(users).build();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving user list", e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Unable to retrieve user list. Please try again later.");
        }
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserByUsername(@PathParam("username") String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Username cannot be empty.");
            }

            Optional<UserDTO> userOptional = userManagementService.findUserByUsername(username);
            if (userOptional.isPresent()) {
                return Response.ok(userOptional.get()).build();
            } else {
                return createErrorResponse(Response.Status.NOT_FOUND,
                    "USER_NOT_FOUND", "User '" + username + "' was not found in the system.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving user: " + username, e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Unable to retrieve user information. Please try again later.");
        }
    }

    @RolesAllowed("ADMIN")
    @POST
    @Path("/{username}/suspend")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response suspendUserAccount(@PathParam("username") String username, SuspendRequestDTO suspendRequest, @Context SecurityContext securityContext) {
        try {
            // Validate input parameters
            if (username == null || username.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Username cannot be empty.");
            }

            if (suspendRequest == null) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Suspend request data is required.");
            }

            if (suspendRequest.reason == null || suspendRequest.reason.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "A reason for suspension is required and cannot be empty.");
            }

            if (suspendRequest.reason.length() > 500) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Suspension reason cannot exceed 500 characters.");
            }

            String adminUsername = securityContext.getUserPrincipal().getName();
            userManagementService.suspendUser(username, adminUsername, suspendRequest.reason);

            return Response.ok()
                .entity("{\"message\":\"User '" + username + "' has been suspended successfully.\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid request to suspend user: " + username, e);
            String message = e.getMessage() != null ? e.getMessage() : "Invalid request parameters.";
            return createErrorResponse(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Cannot suspend user: " + username, e);
            String message = e.getMessage() != null ? e.getMessage() : "Operation not allowed in current state.";
            return createErrorResponse(Response.Status.CONFLICT, "OPERATION_NOT_ALLOWED", message);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error suspending user: " + username, e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Unable to suspend user. Please try again later.");
        }
    }

    @RolesAllowed("ADMIN")
    @POST
    @Path("/{username}/reactivate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reactivateUserAccount(@PathParam("username") String username, @Context SecurityContext securityContext) {
        try {
            // Validate input parameters
            if (username == null || username.trim().isEmpty()) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Username cannot be empty.");
            }

            String adminUsername = securityContext.getUserPrincipal().getName();
            userManagementService.reactivateUser(username, adminUsername);

            return Response.ok()
                .entity("{\"message\":\"User '" + username + "' has been reactivated successfully.\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid request to reactivate user: " + username, e);
            String message = e.getMessage() != null ? e.getMessage() : "Invalid request parameters.";
            return createErrorResponse(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message);
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Cannot reactivate user: " + username, e);
            String message = e.getMessage() != null ? e.getMessage() : "Operation not allowed in current state.";
            return createErrorResponse(Response.Status.CONFLICT, "OPERATION_NOT_ALLOWED", message);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reactivating user: " + username, e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Unable to reactivate user. Please try again later.");
        }
    }

    // *** ADD THIS NEW ENDPOINT ***
    @GET
    @Path("/{username}/audit")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFullUserAudit(
            @PathParam("username") String username,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("25") int size) {

        try {
            UserAuditDTO auditData = userAuditService.getFullUserAudit(username, page, size);
            return Response.ok(auditData).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    @RolesAllowed({"ADMIN", "EMPLOYEE"})
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUsers(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit,
            @QueryParam("accountLevel") String accountLevel,
            @QueryParam("status") String status,
            @QueryParam("kycStatus") String kycStatus,
            @QueryParam("username") String username,
            @QueryParam("email") String email) {

        try {
            // Validate pagination parameters
            if (page < 1) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Page number must be greater than 0.");
            }

            if (limit < 1 || limit > 1000) {
                return createErrorResponse(Response.Status.BAD_REQUEST,
                    "INVALID_INPUT", "Limit must be between 1 and 1000.");
            }

            // Parse enum parameters
            AccountLevel accountLevelEnum = null;
            if (accountLevel != null && !accountLevel.trim().isEmpty()) {
                try {
                    accountLevelEnum = AccountLevel.valueOf(accountLevel.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return createErrorResponse(Response.Status.BAD_REQUEST,
                        "INVALID_INPUT", "Invalid account level. Valid values are: BRONZE, GOLD, PLATINUM, DIAMOND.");
                }
            }

            UserStatus statusEnum = null;
            if (status != null && !status.trim().isEmpty()) {
                try {
                    statusEnum = UserStatus.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return createErrorResponse(Response.Status.BAD_REQUEST,
                        "INVALID_INPUT", "Invalid status. Valid values are: ACTIVE, INACTIVE, SUSPENDED, DEACTIVATED.");
                }
            }

            KycStatus kycStatusEnum = null;
            if (kycStatus != null && !kycStatus.trim().isEmpty()) {
                try {
                    kycStatusEnum = KycStatus.valueOf(kycStatus.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return createErrorResponse(Response.Status.BAD_REQUEST,
                        "INVALID_INPUT", "Invalid KYC status. Valid values are: PENDING, VERIFIED, REJECTED.");
                }
            }

            // Call service method to search users
            List<UserDTO> users = userManagementService.searchUsers(
                page, limit, accountLevelEnum, statusEnum, kycStatusEnum, username, email);

            // Get total count for pagination info
            int totalCount = userManagementService.countUsers(
                accountLevelEnum, statusEnum, kycStatusEnum, username, email);

            UserSearchResponse response = new UserSearchResponse(users, totalCount, page, limit);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid search parameters", e);
            String message = e.getMessage() != null ? e.getMessage() : "Invalid search parameters.";
            return createErrorResponse(Response.Status.BAD_REQUEST, "INVALID_REQUEST", message);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching users", e);
            return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "Unable to search users. Please try again later.");
        }
    }
}