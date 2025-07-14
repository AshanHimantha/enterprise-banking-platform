package rest;


import dto.TransactionRequestDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.TransactionService;

@Path("/transactions")
public class TransactionController {

    @EJB
    private TransactionService transactionService;

    @POST
    @Path("/transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response transferFunds(TransactionRequestDTO transactionRequest, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            transactionService.performTransfer(username, transactionRequest);
            return Response.ok("Transfer successful.").build();
        } catch (Exception e) {
            // Return specific error messages based on the exception type
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}