package enums;

public enum ScheduledPaymentStatus {
    ACTIVE,
    PAUSED,
    COMPLETED, // For schedules with a defined end date
    FAILED       // If a payment consistently fails
}