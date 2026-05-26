package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import java.util.List;

public interface TournamentBracketService {

    List<TournamentMatch> generateBracket(long tournamentId, User actingUser);

    Tournament publishBracket(
            long tournamentId, User actingUser, List<TournamentMatchScheduleRequest> schedules);

    TournamentBracketView getBracket(long tournamentId, User viewer);

    TournamentMatch declareWinner(
            long tournamentId,
            long matchId,
            TournamentWinnerDeclarationRequest request,
            User actingUser);

    List<TournamentTeam> listTeamsForSetup(long tournamentId, User actingUser);

    Tournament updatePairingStrategy(
            long tournamentId, User actingUser, TournamentPairingStrategy pairingStrategy);

    void saveManualPairings(long tournamentId, User actingUser, List<Long> orderedTeamIds);
}
