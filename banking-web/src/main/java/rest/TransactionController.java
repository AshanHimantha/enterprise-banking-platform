package rest;


import dto.TransactionDTO;
import dto.TransactionDetailDTO;
import dto.TransactionRequestDTO;
import enums.TransactionType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.DocumentGenerationService;
import service.TransactionService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Path("/transactions")
public class TransactionController {

    @EJB
    private TransactionService transactionService;

    @EJB // Inject the document service
    private DocumentGenerationService documentGenerationService;

    /**
     * Endpoint to perform a fund transfer.
     */
    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response transferFunds(TransactionRequestDTO transactionRequest, @Context SecurityContext securityContext) {
        try {
            // Get the username of the logged-in user from their JWT
            String username = securityContext.getUserPrincipal().getName();

            // Call the EJB service to perform the transfer
            transactionService.performTransfer(username, transactionRequest);

            return Response.ok(Collections.singletonMap("message", "Transfer successful.")).build();
        } catch (Exception e) {
            // Catch any business logic exceptions (e.g., insufficient funds, invalid account)
            // and return a user-friendly error message.
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/{transactionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response getSingleTransaction(@PathParam("transactionId") Long transactionId, @Context SecurityContext securityContext) {

        String username = securityContext.getUserPrincipal().getName();

        try {
            Optional<TransactionDetailDTO> transactionOptional = transactionService.getTransactionDetails(username, transactionId);

            // Use the modern Optional.map().orElse() pattern for a clean response
            return transactionOptional
                    .map(dto -> Response.ok(dto).build()) // If present, return 200 OK with the DTO
                    .orElse(Response.status(Response.Status.NOT_FOUND).build()); // If empty, return 404 Not Found

        } catch (SecurityException e) {
            // This catches the authorization failure from the service
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/history/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response getHistory(
            @PathParam("accountNumber") String accountNumber,
            @Context SecurityContext securityContext,
            // Use @QueryParam to read optional parameters from the URL
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr,
            @QueryParam("type") TransactionType transactionType,
            @QueryParam("page") @DefaultValue("1") int page, // Default to page 1
            @QueryParam("size") @DefaultValue("20") int size) { // Default to 20 items per page

        try {
            String username = securityContext.getUserPrincipal().getName();

            // Parse date strings into LocalDate objects
            LocalDate startDate = (startDateStr != null) ? LocalDate.parse(startDateStr) : null;
            LocalDate endDate = (endDateStr != null) ? LocalDate.parse(endDateStr) : null;

            List<TransactionDTO> history = transactionService.getTransactionHistory(
                    username, accountNumber, startDate, endDate, transactionType, page, size);

            return Response.ok(history).build();
        } catch (Exception e) {
            System.out.println("Error fetching transaction history: " + e.getMessage());
        }


        return null;
    }

    @GET
    @Path("/{transactionId}/receipt")
    @Produces("application/pdf") // This tells the browser it's a PDF file
    @RolesAllowed("CUSTOMER")
    public Response downloadReceipt(@PathParam("transactionId") Long transactionId, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();

            // Call the service to get the PDF bytes
            ByteArrayOutputStream pdfStream = documentGenerationService.generateTransactionReceiptPdf(username, transactionId);

            if (pdfStream.size() == 0) {
                return Response.status(Response.Status.NOT_FOUND).entity("Could not generate receipt.").build();
            }

            // Build the response to trigger a download
            return Response.ok(pdfStream.toByteArray())
                    .header("Content-Disposition", "attachment; filename=\"receipt-" + transactionId + ".pdf\"")
                    .build();

        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    }