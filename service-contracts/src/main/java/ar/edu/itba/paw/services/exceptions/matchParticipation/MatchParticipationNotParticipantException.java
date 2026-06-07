package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNotParticipantException extends MatchParticipationException {
    public MatchParticipationNotParticipantException() {
        super("notParticipant");
    }
}
