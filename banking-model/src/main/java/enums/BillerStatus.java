package enums;

public enum BillerStatus {
    ACTIVE,         // Biller is available for users to add and pay
    INACTIVE,       // Biller is temporarily disabled and does not appear in lists
    DISCONTINUED    // Biller is permanently no longer supported
}