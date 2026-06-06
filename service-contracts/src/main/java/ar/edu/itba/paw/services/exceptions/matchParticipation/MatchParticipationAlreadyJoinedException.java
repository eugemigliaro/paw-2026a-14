package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationAlreadyJoinedException extends MatchParticipationException {
    public MatchParticipationAlreadyJoinedException() {
        super("join.error.alreadyJoined");
    }
}
