package ocudni104.idp.user.domain.exception;

import ocudni104.idp.user.domain.UserId;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UserId userId) {
        super("User not found: " + userId.value());
    }
}
