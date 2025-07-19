package rest;

import dto.DepositHistoryDTO;
import dto.DepositRequestDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.DepositService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


@Path("/employee/deposits")
@RolesAllowed({"EMPLOYEE", "ADMIN"})
public class DepositController {

    @EJB
    private DepositService depositService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeDeposit(DepositRequestDTO request, @Context SecurityContext securityContext) {
        try {
            String employeeUsername = securityContext.getUserPrincipal().getName();
            depositService.processDeposit(employeeUsername, request);
            return Response.ok(Collections.singletonMap("message", "Deposit successful.")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }


    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHistory(
            @QueryParam("search") String searchTerm,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("25") int size
    ) {
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;

        List<DepositHistoryDTO> history = depositService.getDepositHistory(searchTerm, startDate, endDate, page, size);
        long totalCount = depositService.countDepositHistory(searchTerm, startDate, endDate);

        return Response.ok(history)
                .header("X-Total-Count", totalCount)
                .header("Access-Control-Expose-Headers", "X-Total-Count")
                .build();
    }
}