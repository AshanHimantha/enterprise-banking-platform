package rest;


import auth.service.AdminService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

@Path("/admin")
public class AdminKYCController {

    @EJB
    private AdminService adminService;

    @POST
    @Path("/users/{username}/approve-kyc")
    @RolesAllowed({"ADMIN", "EMPLOYEE"}) // Secure the API endpoint
    public Response approveKyc(@PathParam("username") String username) {
        try {
            adminService.approveKycAndAssignRole(username);
            return Response.ok(Collections.singletonMap("message", "KYC approved and role assigned for user " + username)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", "User not found or operation failed."))
                    .build();
        }
    }
}