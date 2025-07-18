package rest;


import dto.StatementRequestDTO;
import jakarta.ws.rs.*;
import service.AccountService; // Import the service that has our verification method
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.DocumentGenerationService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;

/**
 * JAX-RS Controller for user-facing actions related to account statements.
 */
@Path("/statements")
@RolesAllowed("CUSTOMER")
public class StatementController {

    // Inject the JMS context for sending messages to our queue.
    @Inject
    private JMSContext jmsContext;

    // Look up the JMS queue we configured on the Payara server.
    @Resource(lookup = "jms/statementQueue")
    private Queue statementQueue;

    // Inject the AccountService, which contains our ownership verification logic.
    @EJB
    private AccountService accountService;

    @EJB
    private DocumentGenerationService documentGenerationService;

    /**
     * Endpoint for a user to request an account statement for a specific period.
     * The request is first authorized, then queued for asynchronous processing.
     * The user receives an immediate "Accepted" response.
     */
    @POST
    @Path("/request")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestStatement(StatementRequestDTO request, @Context SecurityContext securityContext) {

        // --- 1. Input Validation ---
        if (request == null || request.getAccountNumber() == null || request.getStartDate() == null || request.getEndDate() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "accountNumber, startDate, and endDate are required."))
                    .build();
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Start date cannot be after end date."))
                    .build();
        }

        // --- 2. Authorization ---
        String username = securityContext.getUserPrincipal().getName();
        try {
            // Call the dedicated method in AccountService to verify ownership.
            // This will throw a SecurityException if the check fails.
            accountService.verifyAccountOwnership(username, request.getAccountNumber());
        } catch (SecurityException e) {
            // If verification fails, return a 403 Forbidden error.
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            // If the account doesn't exist at all.
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }

        // --- 3. Queue the Job ---
        // If authorization passed, we can safely queue the statement generation task.
        try {
            // Format the message exactly as the monthly scheduler does: "accountNumber;startDate;endDate"
            String message = String.format("%s;%s;%s",
                    request.getAccountNumber(),
                    request.getStartDate().toString(),
                    request.getEndDate().toString());

            jmsContext.createProducer().send(statementQueue, message);

            // Return 202 Accepted: This tells the client the request was accepted
            // for background processing. This is the correct HTTP status for this async flow.
            return Response.status(Response.Status.ACCEPTED)
                    .entity(Collections.singletonMap("message", "Your statement request has been accepted and will be emailed to you shortly."))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "Failed to queue the statement request."))
                    .build();
        }
    }

    @GET
    @Path("/{accountNumber}/download")
    @Produces("application/pdf") // This tells JAX-RS the primary success content type.
    public Response downloadStatement(
            @PathParam("accountNumber") String accountNumber,
            @QueryParam("startDate") String startDateStr,
            @QueryParam("endDate") String endDateStr,
            @Context SecurityContext securityContext) {

        // --- 1. Input Validation ---
        if (startDateStr == null || endDateStr == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("startDate and endDate query parameters are required.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startDateStr);
            endDate = LocalDate.parse(endDateStr);
        } catch (java.time.format.DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid date format. Please use YYYY-MM-DD.")
                    .type(MediaType.TEXT_PLAIN).build();
        }

        // --- 2. Authorization ---
        String username = securityContext.getUserPrincipal().getName();
        try {
            accountService.verifyAccountOwnership(username, accountNumber);
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
        }

        // --- 3. Generate the PDF ---
        try {
            ByteArrayOutputStream pdfStream = documentGenerationService.generateAccountStatementPdf(accountNumber, startDate, endDate);

            if (pdfStream.size() == 0) {
                return Response.status(Response.Status.OK)
                        .entity("No transactions found for the selected period. No statement generated.")
                        .type(MediaType.TEXT_PLAIN).build();
            }

            // --- 4. Build the Download Response ---
            String filename = String.format("Statement-%s-%s.pdf", accountNumber, startDate.toString());

            return Response
                    .ok(pdfStream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while generating your statement.")
                    .type(MediaType.TEXT_PLAIN).build();
        }
    }
}