package rest;


import dto.DashboardAccountDTO;
import entity.User;
import enums.AccountType;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import service.AccountService;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/accounts")
public class AccountController {

    // Inject the EJB using its interface. This is a best practice.
    @EJB
    private AccountService accountService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNewUserAndAccount(User user) {
        try {
            accountService.createAccountForNewUser(user, new BigDecimal("100.0"), AccountType.SAVING);
            return Response.status(Response.Status.CREATED)
                    .entity("User and account created successfully for " + user.getUsername())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating account: " + e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/my-accounts")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response getMyAccounts(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        List<DashboardAccountDTO> accounts = accountService.findAccountsByUsername(username);
        return Response.ok(accounts).build();
    }

    /**
     * Endpoint to get the details of a single, specific account.
     */
    @GET
    @Path("/{accountNumber}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response getAccountDetails(@PathParam("accountNumber") String accountNumber, @Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        Optional<DashboardAccountDTO> accountOptional = accountService.findAccountByNumberForUser(accountNumber, username);

        return accountOptional
                .map(Response::ok) // If found, wrap DTO in 200 OK
                .orElse(Response.status(Response.Status.FORBIDDEN)) // If not found, it means user doesn't own it or it doesn't exist
                .build();
    }
}