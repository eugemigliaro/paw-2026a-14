package ar.edu.itba.paw.models.exceptions.registration;

public class EmailTakenException extends AccountRegistrationException {
    public EmailTakenException() {
        super("emailTaken");
    }
}
