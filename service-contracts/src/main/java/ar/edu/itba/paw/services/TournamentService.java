package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.query.InvolvementScope;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TournamentService {

    Tournament createTournament(User host, CreateTournamentRequest request);

    Optional<Tournament> findPublicTournament(long tournamentId);

    Optional<Tournament> findTournamentForHost(long tournamentId, User host);

    Optional<Tournament> findEditableTournamentForHost(long tournamentId, User host);

    TournamentManagementPermissions getManagementPermissions(
            Tournament tournament, User actingUser);

    PaginatedResult<Tournament> searchPublicTournaments(
            String query,
            List<Sport> sport,
            LocalDate startDate,
            LocalDate endDate,
            EventSort sort,
            int page,
            int pageSize,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double latitude,
            Double longitude);

    PaginatedResult<Tournament> findDashboardTournaments(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sport,
            LocalDate startDate,
            LocalDate endDate,
            EventSort sort,
            int page,
            int pageSize,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double latitude,
            Double longitude);

    Tournament update(long tournamentId, User actingUser, UpdateTournamentRequest request);

    Tournament cancel(long tournamentId, User actingUser, String reason);

    Set<Long> findParticipatingTournamentIds(User user, List<Long> tournamentIds);

    TournamentViewerCapabilities viewerCapabilities(Tournament tournament, User viewer);

    PaginatedResult<TournamentMatch> findDashboardTournamentMatches(
            User user,
            Boolean upcoming,
            String query,
            List<Sport> sports,
            List<TournamentMatchStatus> statuses,
            InvolvementScope involvement,
            EventSort sort,
            int page,
            int pageSize);

    List<TournamentTeam> findBracketTeams(long tournamentId);

    List<TournamentTeam> findBracketTeams(Collection<Long> tournamentIds);
}
