package rest;


import dto.StatementRequestDTO;
import service.AccountService; // Import the service that has our verification method
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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
}