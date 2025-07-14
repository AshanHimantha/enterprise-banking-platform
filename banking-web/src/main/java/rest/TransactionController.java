package rest;


import dto.TransactionDTO;
import dto.TransactionRequestDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.TransactionService;

import java.util.Collections;
import java.util.List;

@Path("/transactions")
public class TransactionController {

    @EJB
    private TransactionService transactionService;

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

    /**
     * Endpoint to get the transaction history for a specific account.
     */
    @GET
    @Path("/history/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response getHistory(@PathParam("accountNumber") String accountNumber, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();

            // Call the service to get the history
            List<TransactionDTO> history = transactionService.getTransactionHistory(username, accountNumber);

            return Response.ok(history).build();
        } catch (SecurityException e) {
            // This is thrown if the user tries to access an account they don't own
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }
}