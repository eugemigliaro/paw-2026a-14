package ar.edu.itba.paw.models.exceptions.matchParticipation;

public class MatchParticipationSeriesAlreadyInvitedException extends MatchParticipationException {
    public MatchParticipationSeriesAlreadyInvitedException() {
        super("seriesAlreadyInvited");
    }
}
