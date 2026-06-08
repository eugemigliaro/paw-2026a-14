package ar.edu.itba.paw.models.exceptions.registration;

public class EmailPendingVerificationException extends AccountRegistrationException {
    public EmailPendingVerificationException() {
        super("emailPending");
    }
}
