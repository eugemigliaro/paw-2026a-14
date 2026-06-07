package ar.edu.itba.paw.models.exceptions.registration;

public class EmailInvalidException extends AccountRegistrationException {
    public EmailInvalidException() {
        super("emailInvalid");
    }
}
