package ar.edu.itba.paw.services.exceptions;

import org.springframework.lang.NonNull;

public class PasswordResetException extends RuntimeException {

    @NonNull private final String code;

    public PasswordResetException(@NonNull final String code, @NonNull final String message) {
        super(message);
        this.code = code;
    }

    @NonNull
    public String getCode() {
        return code;
    }
}
