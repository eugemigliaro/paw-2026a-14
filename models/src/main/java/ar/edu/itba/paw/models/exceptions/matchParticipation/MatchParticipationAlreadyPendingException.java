package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationAlreadyPendingException extends MatchParticipationException {
    public MatchParticipationAlreadyPendingException() {
        super("alreadyPending");
    }
}
