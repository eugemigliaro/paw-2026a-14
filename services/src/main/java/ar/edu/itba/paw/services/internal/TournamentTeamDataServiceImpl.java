package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TournamentTeamDataServiceImpl implements TournamentTeamDataService {

    private final TournamentTeamDao tournamentTeamDao;

    public TournamentTeamDataServiceImpl(final TournamentTeamDao tournamentTeamDao) {
        this.tournamentTeamDao = Objects.requireNonNull(tournamentTeamDao);
    }

    @Override
    public TournamentTeam create(
            final Tournament tournament,
            final String name,
            final TournamentTeamOrigin origin,
            final Integer seedPosition) {
        return tournamentTeamDao.create(tournament, name, origin, seedPosition);
    }

    @Override
    public TournamentTeamMember addMember(
            final TournamentTeam team, final User user, final boolean captain) {
        return tournamentTeamDao.addMember(team, user, captain);
    }

    @Override
    public void removeMember(final TournamentTeam team, final User user) {
        tournamentTeamDao.removeMember(team, user);
    }

    @Override
    public void delete(final TournamentTeam team) {
        tournamentTeamDao.delete(team);
    }

    @Override
    public long countMembers(final long teamId) {
        return tournamentTeamDao.countMembers(teamId);
    }

    @Override
    public long countMembersByTournament(final long tournamentId) {
        return tournamentTeamDao.countMembersByTournament(tournamentId);
    }

    @Override
    public List<TournamentTeam> findJoinableByTournament(
            final long tournamentId, final int teamSize) {
        return tournamentTeamDao.findJoinableByTournament(tournamentId, teamSize);
    }

    @Override
    public Optional<TournamentTeam> findById(final long teamId) {
        return tournamentTeamDao.findById(teamId);
    }

    @Override
    public List<TournamentTeam> findByTournament(final long tournamentId) {
        return tournamentTeamDao.findByTournament(tournamentId);
    }

    @Override
    public List<TournamentTeam> findByTournamentUnordered(final long tournamentId) {
        return tournamentTeamDao.findByTournamentUnordered(tournamentId);
    }

    @Override
    public List<TournamentTeam> findSeededByTournament(final long tournamentId) {
        return tournamentTeamDao.findSeededByTournament(tournamentId);
    }

    @Override
    public List<TournamentTeamMember> findMembersByTournament(final long tournamentId) {
        return tournamentTeamDao.findMembersByTournament(tournamentId);
    }

    @Override
    public Optional<TournamentTeam> findUserTeam(final long tournamentId, final long userId) {
        return tournamentTeamDao.findUserTeam(tournamentId, userId);
    }

    @Override
    public List<Tournament> findTournamentsByMember(final User user) {
        return tournamentTeamDao.findTournamentsByMember(user);
    }

    @Override
    public void saveSeedOrder(final List<TournamentTeam> teams, final List<Long> orderedTeamIds) {
        tournamentTeamDao.saveSeedOrder(teams, orderedTeamIds);
    }

    @Override
    public long countByTournament(final long tournamentId) {
        return tournamentTeamDao.countByTournament(tournamentId);
    }
}
