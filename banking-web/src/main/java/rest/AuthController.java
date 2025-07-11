package rest;


import auth.service.AuthService;
import entity.User;
import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import rest.dto.LoginDTO;
import java.util.Collections;
import java.util.Optional;


// Base path for all authentication-related endpoints
@Path("/auth")
public class AuthController {

    @EJB
    private AuthService authService;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(User user) {
        try {
            // For now, all new users are registered as CUSTOMER
            authService.registerUser(user, "CUSTOMER");
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("message", "User registered successfully"))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(LoginDTO loginDTO) {
        Optional<String> tokenOptional = authService.login(loginDTO.getUsername(), loginDTO.getPassword());

        if (tokenOptional.isPresent()) {
            // If login is successful, return the JWT in the response
            String token = tokenOptional.get();
            return Response.ok(Collections.singletonMap("token", token)).build();
        } else {
            // If login fails, return a 401 Unauthorized error
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Collections.singletonMap("error", "Invalid username or password"))
                    .build();
        }
    }
}