package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNoPendingRequestException extends MatchParticipationException {
    public MatchParticipationNoPendingRequestException() {
        super("noPendingRequest");
    }
}
