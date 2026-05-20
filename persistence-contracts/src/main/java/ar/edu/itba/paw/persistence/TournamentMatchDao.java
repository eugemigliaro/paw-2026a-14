package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import java.util.List;
import java.util.Optional;

public interface TournamentMatchDao {

    TournamentMatch create(
            Tournament tournament,
            int roundNumber,
            int matchIndex,
            TournamentTeam teamA,
            TournamentTeam teamB,
            TournamentMatchStatus status,
            TournamentMatch parentMatchA,
            TournamentMatch parentMatchB);

    List<TournamentMatch> findByTournament(long tournamentId);

    Optional<TournamentMatch> findById(long matchId);

    Optional<TournamentMatch> findByTournamentAndId(long tournamentId, long matchId);

    List<TournamentMatch> findByTournamentAndRound(long tournamentId, int roundNumber);

    TournamentMatch update(TournamentMatch match);
}
