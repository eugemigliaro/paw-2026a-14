package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.util.List;
import java.util.Optional;

public interface TournamentTeamDao {

    TournamentTeam create(
            Tournament tournament, String name, TournamentTeamOrigin origin, Integer seedPosition);

    TournamentTeamMember addMember(TournamentTeam team, User user, boolean captain);

    Optional<TournamentTeam> findById(long teamId);

    List<TournamentTeam> findByTournament(long tournamentId);

    List<TournamentTeam> findSeededByTournament(long tournamentId);

    List<TournamentTeamMember> findMembersByTournament(long tournamentId);

    Optional<TournamentTeam> findUserTeam(long tournamentId, long userId);

    long countByTournament(long tournamentId);
}
