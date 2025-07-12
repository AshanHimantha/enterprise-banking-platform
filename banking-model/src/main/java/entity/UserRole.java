package entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(
        name = "user_role",
        // Add a unique constraint to prevent assigning the same role to the same user twice
        uniqueConstraints = @UniqueConstraint(columnNames = {"username", "rolename"})
)
public class UserRole implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "rolename", nullable = false)
    private String rolename;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }
}