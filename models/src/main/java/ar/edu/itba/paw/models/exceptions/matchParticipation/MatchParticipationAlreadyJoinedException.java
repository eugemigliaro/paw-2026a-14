package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationAlreadyJoinedException extends MatchParticipationException {
    public MatchParticipationAlreadyJoinedException() {
        super("alreadyJoined");
    }
}
