package rest;


import auth.service.SearchService;
import dto.UserSearchResultDTO;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/search")
@RolesAllowed({"ADMIN", "EMPLOYEE","CUSTOMER"})
public class SearchController {

    @EJB
    private SearchService searchService;

    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchUsers(
            @QueryParam("email") String email,
            @QueryParam("username") String username,
            @QueryParam("accountno") String accountNumber) {



        // Determine which search term to use. We'll prioritize them.
        String searchTerm = null;
        if (email != null && !email.trim().isEmpty()) {
            searchTerm = email;
        } else if (username != null && !username.trim().isEmpty()) {
            searchTerm = username;
        } else if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            searchTerm = accountNumber;
        }

        if (searchTerm == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("A search parameter (email, username, or accountno) is required.")
                    .build();
        }

        List<UserSearchResultDTO> results = searchService.searchUsers(searchTerm);
        return Response.ok(results).build();
    }
}