package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import java.util.List;

public class TournamentBracketView {

    private final Tournament tournament;
    private final List<TournamentTeam> teams;
    private final List<TournamentMatch> matches;
    private final TournamentTeam viewerTeam;
    private final TournamentMatch focusedMatch;

    public TournamentBracketView(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final List<TournamentMatch> matches,
            final TournamentTeam viewerTeam,
            final TournamentMatch focusedMatch) {
        this.tournament = tournament;
        this.teams = List.copyOf(teams);
        this.matches = List.copyOf(matches);
        this.viewerTeam = viewerTeam;
        this.focusedMatch = focusedMatch;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public List<TournamentTeam> getTeams() {
        return teams;
    }

    public List<TournamentMatch> getMatches() {
        return matches;
    }

    public TournamentTeam getViewerTeam() {
        return viewerTeam;
    }

    public TournamentMatch getFocusedMatch() {
        return focusedMatch;
    }
}
