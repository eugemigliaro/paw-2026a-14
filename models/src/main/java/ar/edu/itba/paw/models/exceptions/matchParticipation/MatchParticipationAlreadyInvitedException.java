package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationAlreadyInvitedException extends MatchParticipationException {
    public MatchParticipationAlreadyInvitedException() {
        super("alreadyInvited");
    }
}
