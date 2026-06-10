package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.util.List;
import java.util.Optional;

public interface TournamentTeamDataService {

    TournamentTeam create(
            Tournament tournament, String name, TournamentTeamOrigin origin, Integer seedPosition);

    TournamentTeamMember addMember(TournamentTeam team, User user, boolean captain);

    void removeMember(TournamentTeam team, User user);

    void delete(TournamentTeam team);

    long countMembers(long teamId);

    long countMembersByTournament(long tournamentId);

    List<TournamentTeam> findJoinableByTournament(long tournamentId, int teamSize);

    boolean existsByTournamentAndName(long tournamentId, String name);

    Optional<TournamentTeam> findById(long teamId);

    List<TournamentTeam> findByTournament(long tournamentId);

    List<TournamentTeam> findByTournamentUnordered(long tournamentId);

    List<TournamentTeam> findSeededByTournament(long tournamentId);

    List<TournamentTeamMember> findMembersByTournament(long tournamentId);

    Optional<TournamentTeam> findUserTeam(long tournamentId, long userId);

    List<Tournament> findTournamentsByMember(User user);

    void saveSeedOrder(List<TournamentTeam> teams, List<Long> orderedTeamIds);

    long countByTournament(long tournamentId);
}
