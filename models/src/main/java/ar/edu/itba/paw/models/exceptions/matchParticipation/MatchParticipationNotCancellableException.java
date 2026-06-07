package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationNotCancellableException extends MatchParticipationException {
    public MatchParticipationNotCancellableException() {
        super("notCancellable");
    }
}
