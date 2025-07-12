package rest;


import auth.service.AuthService;
import dto.EmailVerificationDTO;
import dto.RegisterDTO;
import entity.User;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import dto.LoginDTO;
import java.util.Collections;
import java.util.Optional;


// Base path for all authentication-related endpoints
@RequestScoped
@Path("/auth")
public class AuthController {

    @EJB
    private AuthService authService;

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(RegisterDTO registerDTO) { // Use the DTO
        try {
            authService.registerUser(registerDTO);
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("message", "User registered successfully"))
                    .build();
        } catch (Exception e) {
            // This could be a unique constraint violation (e.g., username already exists)
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Registration failed: " + e.getMessage()))
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


    @POST
    @Path("/verify-email")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyEmail(EmailVerificationDTO verificationDTO) {
        boolean isVerified = authService.verifyEmail(verificationDTO);

        if (isVerified) {
            return Response.ok(Collections.singletonMap("message", "Email verified successfully.")).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid email or verification code."))
                    .build();
        }
    }





    }