package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNoInvitationException extends MatchParticipationException {
    public MatchParticipationNoInvitationException() {
        super("exception.participant.noPendingInvitation");
    }
}
