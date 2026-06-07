package ar.edu.itba.paw.services.exceptions.matchParticipation;

public class MatchParticipationNotInviteOnlyException extends MatchParticipationException {
    public MatchParticipationNotInviteOnlyException() {
        super("notInviteOnly");
    }
}
