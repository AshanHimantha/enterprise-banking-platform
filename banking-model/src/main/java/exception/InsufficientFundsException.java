package exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
