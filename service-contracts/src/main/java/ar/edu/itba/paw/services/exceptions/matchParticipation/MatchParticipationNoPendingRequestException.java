package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNoPendingRequestException extends MatchParticipationException {
    public MatchParticipationNoPendingRequestException(final String message) {
        super(message);
    }
}
