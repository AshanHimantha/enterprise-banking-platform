package rest;

import dto.BillPaymentRequestDTO;
import dto.BillerDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.BilllerService;
import service.TransactionService;

import java.util.Collections;
import java.util.List;

@Path("/bills")
@RolesAllowed("CUSTOMER")
public class BillPaymentController {

    @EJB
    private TransactionService transactionService;

    @EJB
    private BilllerService billlerService;

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllBillers() {
        List<BillerDTO> billers = billlerService.getAllBillers();
        return Response.ok(billers).build();
    }


}