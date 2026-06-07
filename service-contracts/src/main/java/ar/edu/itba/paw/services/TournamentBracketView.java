package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import java.util.List;
import java.util.Map;

public class TournamentBracketView {

    private final Tournament tournament;
    private final List<TournamentTeam> teams;
    private final List<TournamentMatch> matches;
    private final List<TournamentTeamMember> teamMembers;
    private final Map<Long, Boolean> resultRecordableByMatchId;
    private final TournamentTeam viewerTeam;
    private final TournamentMatch focusedMatch;

    public TournamentBracketView(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final List<TournamentMatch> matches,
            final TournamentTeam viewerTeam,
            final TournamentMatch focusedMatch) {
        this(tournament, teams, matches, viewerTeam, focusedMatch, List.of(), Map.of());
    }

    public TournamentBracketView(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final List<TournamentMatch> matches,
            final TournamentTeam viewerTeam,
            final TournamentMatch focusedMatch,
            final List<TournamentTeamMember> teamMembers) {
        this(tournament, teams, matches, viewerTeam, focusedMatch, teamMembers, Map.of());
    }

    public TournamentBracketView(
            final Tournament tournament,
            final List<TournamentTeam> teams,
            final List<TournamentMatch> matches,
            final TournamentTeam viewerTeam,
            final TournamentMatch focusedMatch,
            final List<TournamentTeamMember> teamMembers,
            final Map<Long, Boolean> resultRecordableByMatchId) {
        this.tournament = tournament;
        this.teams = List.copyOf(teams);
        this.matches = List.copyOf(matches);
        this.teamMembers = teamMembers == null ? List.of() : List.copyOf(teamMembers);
        this.resultRecordableByMatchId =
                resultRecordableByMatchId == null
                        ? Map.of()
                        : Map.copyOf(resultRecordableByMatchId);
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

    public List<TournamentTeamMember> getTeamMembers() {
        return teamMembers;
    }

    public boolean isResultRecordable(final Long matchId) {
        return resultRecordableByMatchId.getOrDefault(matchId, false);
    }

    public TournamentTeam getViewerTeam() {
        return viewerTeam;
    }

    public TournamentMatch getFocusedMatch() {
        return focusedMatch;
    }
}
