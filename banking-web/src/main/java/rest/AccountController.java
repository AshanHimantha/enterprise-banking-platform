package rest;


import entity.User;
import enums.AccountType;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import service.AccountService;


import java.math.BigDecimal;

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
}