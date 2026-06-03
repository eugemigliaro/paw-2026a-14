package ar.edu.itba.paw.services.exceptions.tournamentRegistration;

public class TournamentRegistrationInvalidUserException extends RuntimeException {

    public TournamentRegistrationInvalidUserException(String message) {
        super(message);
    }
}
