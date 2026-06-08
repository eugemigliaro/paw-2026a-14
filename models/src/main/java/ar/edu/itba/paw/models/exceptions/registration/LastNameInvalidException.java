package ar.edu.itba.paw.models.exceptions.registration;

public class LastNameInvalidException extends AccountRegistrationException {
    public LastNameInvalidException() {
        super("lastNameInvalid");
    }
}
