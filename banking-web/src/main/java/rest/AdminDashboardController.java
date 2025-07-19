package rest;


import service.AdminDashboardService;
import dto.AdminDashboardDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin/dashboard")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class AdminDashboardController {

    @EJB
    private AdminDashboardService dashboardService;

    @GET
    @Path("/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardSummary() {
        try {
            AdminDashboardDTO summary = dashboardService.getDashboardSummary();
            return Response.ok(summary).build();
        } catch (Exception e) {
            // Handle potential database or other errors
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to load dashboard summary.")
                    .build();
        }
    }
}