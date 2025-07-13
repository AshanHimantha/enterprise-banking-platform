package rest;


import auth.service.AuthService;
import dto.EmailVerificationDTO;
import dto.RegisterDTO;
import dto.TokenUpdateDTO;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import dto.LoginDTO;
import dto.LoginVerificationDTO;
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
        try {
            // Call login service - it now returns Optional<String>
            // Empty Optional = invalid credentials
            // Optional with "VERIFICATION_CODE_SENT" = valid credentials, verification code sent
            // Optional with "{\"error\": \"status\"}" = valid credentials but inactive status
            Optional<String> result = authService.login(loginDTO.getUsername(), loginDTO.getPassword());

            if (result.isPresent()) {
                String response = result.get();

                if ("VERIFICATION_CODE_SENT".equals(response)) {
                    // Valid credentials, verification code was sent
                    return Response.ok(Collections.singletonMap("message", "Verification code sent to your email. Please check your inbox.")).build();
                } else if ("INACTIVE".equals(response) || "SUSPENDED".equals(response) || "DEACTIVATED".equals(response)) {
                    // Valid credentials but user status is not active
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Collections.singletonMap("error", response))
                            .build();
                } else {
                    // Other response types (if any)
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Collections.singletonMap("error", "Unexpected response from login service"))
                            .build();
                }
            } else {
                // Invalid credentials (empty Optional)
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Collections.singletonMap("error", "Invalid username or password"))
                        .build();
            }

        } catch (Exception e) {
            // If login fails due to other errors
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "Login failed: " + e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/verify-login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyLogin(LoginVerificationDTO verificationDTO) {
        Optional<String> tokenOptional = authService.verifyLoginCode(verificationDTO.getUsername(), verificationDTO.getCode());

        if (tokenOptional.isPresent()) {
            // If verification is successful, return the JWT token
            String token = tokenOptional.get();
            return Response.ok(Collections.singletonMap("token", token)).build();
        } else {
            // If verification fails
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid verification code"))
                    .build();
        }
    }


    @POST
    @Path("/verify-email")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyEmail(EmailVerificationDTO verificationDTO) {
        Optional<String> tokenOptional = authService.verifyEmail(verificationDTO);

        if (tokenOptional.isPresent()) {
            String token = tokenOptional.get();
            return Response.ok(Collections.singletonMap("token", token)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error", "Invalid email or verification code."))
                    .build();
        }
    }

    @POST
    @Path("/refresh-token")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateJwtToken(TokenUpdateDTO tokenUpdateDTO) {
        try {
            Optional<String> newTokenOptional = authService.updateJwtToken(tokenUpdateDTO);

            if (newTokenOptional.isPresent()) {
                String newToken = newTokenOptional.get();
                return Response.ok(Collections.singletonMap("token", newToken)).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Collections.singletonMap("error", "Invalid or expired token"))
                        .build();
            }
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Collections.singletonMap("error", "Token update failed: " + e.getMessage()))
                    .build();
        }
    }

    }
