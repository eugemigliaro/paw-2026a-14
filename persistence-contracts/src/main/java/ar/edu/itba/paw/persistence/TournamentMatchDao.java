package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.InvolvementScope;
import ar.edu.itba.paw.models.types.Sport;
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

    List<TournamentMatch> findByUserParticipant(
            User user,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<TournamentMatchStatus> statuses,
            InvolvementScope involvement,
            EventSort sort,
            int offset,
            int limit);

    int countByUserParticipant(
            User user,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<TournamentMatchStatus> statuses,
            InvolvementScope involvement);
}
