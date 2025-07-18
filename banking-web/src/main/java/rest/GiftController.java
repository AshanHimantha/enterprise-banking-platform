package rest;

import service.GiftService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Collections;

@Path("/gifts")
public class GiftController {

    @EJB
    private GiftService giftService;

    @POST
    @Path("/welcome/claim")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("CUSTOMER")
    public Response claimGift(@Context SecurityContext securityContext) {
        try {
            String username = securityContext.getUserPrincipal().getName();
            giftService.claimWelcomeGift(username);
            return Response.ok(Collections.singletonMap("message", "Congratulations! $100.00 has been added to your account.")).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }
}