package auth.security;


import auth.util.TokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// @ApplicationScoped makes this a single, container-managed bean.
@ApplicationScoped
public class JwtAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    private TokenProvider tokenProvider;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {

        // 1. Extract the "Authorization" header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 2. Extract the token from the header
            String token = authHeader.substring(7);

            // 3. Validate the token
            if (tokenProvider.validateToken(token)) {
                // 4. If valid, extract user details (principal and roles)
                Claims claims = tokenProvider.getClaimsFromToken(token);
                String username = claims.getSubject();
                // Assuming roles are stored as a List in the token's claims
                List<String> roles = claims.get("roles", List.class);
                Set<String> rolesSet = new HashSet<>(roles);

                // 5. Notify the container of a successful authentication
                // This is the most important step. It populates the SecurityContext.
                return httpMessageContext.notifyContainerAboutLogin(username, rolesSet);
            }
        }

        // If no token or invalid token, let the container know to continue,
        // but the user will be unauthenticated.
        return httpMessageContext.doNothing();
    }
}