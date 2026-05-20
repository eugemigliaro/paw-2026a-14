package ar.edu.itba.paw.services;

public class TournamentWinnerDeclarationRequest {

    private final long winnerTeamId;

    public TournamentWinnerDeclarationRequest(final long winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public long getWinnerTeamId() {
        return winnerTeamId;
    }
}
