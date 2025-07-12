package dto;

public class TokenUpdateDTO {
    private String currentToken;
    private String username;

    // Default constructor
    public TokenUpdateDTO() {}

    // Constructor with parameters
    public TokenUpdateDTO(String currentToken, String username) {
        this.currentToken = currentToken;
        this.username = username;
    }

    // Getters and Setters
    public String getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(String currentToken) {
        this.currentToken = currentToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
