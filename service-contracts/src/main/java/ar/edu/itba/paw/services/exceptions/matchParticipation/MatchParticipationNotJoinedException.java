package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNotJoinedException extends MatchParticipationException {
    public MatchParticipationNotJoinedException() {
        super("notJoined");
    }
}
