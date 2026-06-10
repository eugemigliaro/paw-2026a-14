package ar.edu.itba.paw.models.exceptions.tournamentRegistration;

public class TournamentRegistrationTeamFullException extends TournamentRegistrationException {
    public TournamentRegistrationTeamFullException() {
        super("teamFull");
    }
}
