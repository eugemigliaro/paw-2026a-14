package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationNotJoinedException extends MatchParticipationException {
    public MatchParticipationNotJoinedException() {
        super("notJoined");
    }
}
