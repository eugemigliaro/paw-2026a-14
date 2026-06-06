package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationIsHostException extends MatchParticipationException {
    public MatchParticipationIsHostException() {
        super("exception.event.isHost");
    }
}
