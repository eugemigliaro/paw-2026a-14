package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationAlreadyPendingException extends MatchParticipationException {
    public MatchParticipationAlreadyPendingException() {
        super("alreadyPending");
    }
}
