package entity;


import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "saved_biller")
public class SavedBiller implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    public Biller getBiller() {
        return biller;
    }

    public void setBiller(Biller biller) {
        this.biller = biller;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserReferenceNumber() {
        return userReferenceNumber;
    }

    public void setUserReferenceNumber(String userReferenceNumber) {
        this.userReferenceNumber = userReferenceNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_id", nullable = false)
    private Biller biller;


    @Column(nullable = false)
    private String userReferenceNumber;

    private String nickname;


}