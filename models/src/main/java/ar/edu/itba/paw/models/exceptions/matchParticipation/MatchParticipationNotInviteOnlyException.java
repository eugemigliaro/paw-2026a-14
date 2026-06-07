package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationNotInviteOnlyException extends MatchParticipationException {
    public MatchParticipationNotInviteOnlyException() {
        super("notInviteOnly");
    }
}
