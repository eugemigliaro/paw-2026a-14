package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationUnauthenticatedException extends MatchParticipationException {
    public MatchParticipationUnauthenticatedException(final String message) {
        super(message);
    }
}
