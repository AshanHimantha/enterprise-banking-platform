package dto;


public class BillerStatusUpdateDTO {

    private String status;

    /**
     * A no-argument constructor is required for JAX-RS and other frameworks
     * to instantiate the object when deserializing JSON.
     */
    public BillerStatusUpdateDTO() {
    }

    // --- Getters and Setters ---
    // These are essential for the JSON mapping library to work.

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}