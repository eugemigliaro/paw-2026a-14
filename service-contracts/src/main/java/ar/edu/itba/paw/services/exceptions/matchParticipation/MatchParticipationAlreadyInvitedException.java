package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationAlreadyInvitedException extends MatchParticipationException {
    public MatchParticipationAlreadyInvitedException() {
        super("host.invites.error.alreadyInvited");
    }
}
