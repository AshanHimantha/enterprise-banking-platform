package rest;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import javax.crypto.SecretKey;
import java.util.Date;

@WebFilter("/*")
public class CorsFilter implements Filter {

    // Use the same secret key as in TokenProvider
    private final String secretKey = "iTPBvXP8RrKllw1vSqfUB5pZl5ul6t9foiVCIjtZGQe2r7w4";
    private final SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        System.out.println("Received request path: " + httpRequest.getRequestURI());
        // Get the origin from the request
        String origin = httpRequest.getHeader("Origin");

        // Set CORS headers - allow specific origins for JWT auth
        if (origin != null && (origin.equals("http://localhost:3000") ||
                               origin.equals("https://baking-webapp.vercel.app") ||
                               origin.equals("http://127.0.0.1:3000") ||
                               origin.equals("https://orbinbank.ashanhimantha.com"))) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            // Fallback for other origins
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "false");
        }

        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, X-Auth-Token");
        httpResponse.setHeader("Access-Control-Expose-Headers", "Authorization, X-Auth-Token");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Check for JWT token expiration
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Parse the token to check expiration
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Check if token is expired
                Date expiration = claims.getExpiration();
                if (expiration != null && expiration.before(new Date())) {
                    // Token is expired, send 401
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"JWT token has expired\",\"code\":\"TOKEN_EXPIRED\"}");
                    return;
                }
            } catch (ExpiredJwtException e) {
                // Token is expired
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"JWT token has expired\",\"code\":\"TOKEN_EXPIRED\"}");
                return;
            } catch (JwtException e) {
                // Invalid token (malformed, signature invalid, etc.)
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Invalid JWT token\",\"code\":\"INVALID_TOKEN\"}");
                return;
            } catch (Exception e) {
                // Other token-related errors
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\":\"Token validation failed\",\"code\":\"TOKEN_VALIDATION_FAILED\"}");
                return;
            }
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}
