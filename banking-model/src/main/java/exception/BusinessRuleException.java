package exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}