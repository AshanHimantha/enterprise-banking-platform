package auth.util;


import entity.User;
import entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class TokenProvider {

    // IMPORTANT: In a real application, this secret key should be externalized
    // to a secure configuration file or environment variable.
    // Using a strong secret key with at least 256 bits (32 bytes)
    private final String secretKey = "iTPBvXP8RrKllw1vSqfUB5pZl5ul6t9foiVCIjtZGQe2r7w4"; // 48 characters - very strong key
    private final Key key = Keys.hmacShaKeyFor(secretKey.getBytes());
    private final long tokenValidityInMilliseconds = 3600000; // 1 hour

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    /**
     * Creates a JWT for a given user, including their roles as a claim.
     */
    public String createToken(User user) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        // Fetch user roles from the database
        List<String> roles = findRolesByUsername(user.getUsername());

        // Build the JWT with additional user information as claims
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", roles) // Add roles as a custom claim
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("kycStatus", user.getKycStatus() != null ? user.getKycStatus().toString() : null)
                .claim("profilePictureUrl", user.getProfilePictureUrl())
                .issuedAt(new Date())
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * Validates a token. Returns true if the token is valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            // Updated to use the new API style in JJWT 0.12.x
            Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.err.println("Invalid JWT token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts all claims from a validated token.
     */
    public Claims getClaimsFromToken(String token) {
        // Updated to use the new API style in JJWT 0.12.x
        return Jwts.parser()
                .verifyWith((SecretKey) key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Helper method to find roles for a user.
     * This logic could also be in a dedicated UserService.
     */
    private List<String> findRolesByUsername(String username) {
        TypedQuery<UserRole> query = em.createQuery("SELECT r FROM UserRole r WHERE r.username = :username", UserRole.class);
        query.setParameter("username", username);
        return query.getResultStream()
                .map(UserRole::getRolename) // Assumes UserRole has a getRolename() method
                .collect(Collectors.toList());
    }

    /**
     * Extract user information from JWT token claims
     */
    public UserInfo extractUserInfo(String token) {
        if (!validateToken(token)) {
            return null;
        }

        Claims claims = getClaimsFromToken(token);
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(claims.getSubject());
        userInfo.setEmail(claims.get("email", String.class));
        userInfo.setFirstName(claims.get("firstName", String.class));
        userInfo.setLastName(claims.get("lastName", String.class));
        userInfo.setKycStatus(claims.get("kycStatus", String.class));
        userInfo.setProfilePictureUrl(claims.get("profilePictureUrl", String.class));
        userInfo.setRoles(claims.get("roles", List.class));

        return userInfo;
    }

    /**
     * Inner class to hold user information extracted from JWT
     */
    public static class UserInfo {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String kycStatus;
        private String profilePictureUrl;
        private List<String> roles;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getKycStatus() { return kycStatus; }
        public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

        public String getProfilePictureUrl() { return profilePictureUrl; }
        public void setProfilePictureUrl(String profilePictureUrl) { this.profilePictureUrl = profilePictureUrl; }

        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}