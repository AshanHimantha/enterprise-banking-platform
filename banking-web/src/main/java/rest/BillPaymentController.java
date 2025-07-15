package rest;

import dto.BillPaymentRequestDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.TransactionService;

import java.util.Collections;

@Path("/bills")
@RolesAllowed("CUSTOMER")
public class BillPaymentController {

    @EJB
    private TransactionService transactionService;

    @POST
    @Path("/pay")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response makeOneTimePayment(BillPaymentRequestDTO paymentRequest, @Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            transactionService.payBill(username, paymentRequest);
            return Response.ok(Collections.singletonMap("message", "Bill payment successful.")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }
}