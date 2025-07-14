package rest;

import dto.DashboardSummaryDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.DashboardService;

@Path("/dashboard")
@RolesAllowed("CUSTOMER")
public class DashboardController {

    @EJB
    private DashboardService dashboardService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboardData(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();

        DashboardSummaryDTO summary = dashboardService.getDashboardSummary(username);

        return Response.ok(summary).build();
    }
}
