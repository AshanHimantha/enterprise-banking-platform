package rest;

import dto.AdminTransactionDTO;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.TransactionMonitoringService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Path("/admin/transactions")
@RolesAllowed({"ADMIN", "EMPLOYEE"})
public class TransactionMonitorController {

    @EJB
    private TransactionMonitoringService monitoringService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactions(
            @QueryParam("search") String searchTerm,
            @QueryParam("type") TransactionType transactionType,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("25") int size
    ) {
        // Parse dates from strings
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;

        // Fetch the data and the total count
        List<AdminTransactionDTO> transactions = monitoringService.searchTransactions(
                searchTerm, transactionType, startDate, endDate, page, size);
        long totalCount = monitoringService.countTransactions(
                searchTerm, transactionType, startDate, endDate);

        // Return the data along with pagination headers or in a structured response
        return Response.ok(transactions)
                .header("X-Total-Count", totalCount) // Custom header for total items
                .build();
    }



}