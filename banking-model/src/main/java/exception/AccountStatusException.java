package exception;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class AccountStatusException extends RuntimeException {
    public AccountStatusException(String message) {
        super(message);
    }
}