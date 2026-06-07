package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNotCancellableException extends MatchParticipationException {
    public MatchParticipationNotCancellableException() {
        super("notCancellable");
    }
}
