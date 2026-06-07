package ar.edu.itba.paw.models.exceptions.registration;

public class NameInvalidException extends AccountRegistrationException {
    public NameInvalidException() {
        super("nameInvalid");
    }
}
