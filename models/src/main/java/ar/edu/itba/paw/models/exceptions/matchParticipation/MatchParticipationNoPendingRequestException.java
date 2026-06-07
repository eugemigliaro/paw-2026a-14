package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationNoPendingRequestException extends MatchParticipationException {
    public MatchParticipationNoPendingRequestException() {
        super("noPendingRequest");
    }
}
