package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationNoInvitationException extends MatchParticipationException {
    public MatchParticipationNoInvitationException() {
        super("noInvitation");
    }
}
