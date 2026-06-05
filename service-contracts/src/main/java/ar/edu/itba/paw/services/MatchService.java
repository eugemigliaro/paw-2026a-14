package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.ParticipantStatus;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface MatchService {

    Match createMatch(CreateMatchRequest request);

    Optional<Match> findMatchById(Long matchId);

    Optional<Match> findVisibleMatchById(Long matchId, User viewer);

    boolean canViewMatch(Match match, User viewer);

    MatchManagementPermissions getMatchManagementPermissions(Match match, User viewer);

    Optional<Match> findPublicMatchById(Long matchId);

    Match findEditableMatchForHost(Long matchId, User actingUser);

    Match findEditableRecurringMatchForHost(Long matchId, User actingUser);

    List<Match> findSeriesOccurrences(Long seriesId);

    PaginatedResult<Match> findSeriesOccurrencesPage(Long seriesId, int page, int pageSize);

    List<User> findConfirmedParticipants(Long matchId);

    Match updateMatch(Long matchId, User actingUser, UpdateMatchRequest request);

    List<Match> updateSeriesFromOccurrence(
            Long matchId, User actingUser, UpdateMatchRequest request);

    Match cancelMatch(Long matchId, User actingUser);

    List<Match> cancelSeriesFromOccurrence(Long matchId, User actingUser);

    PaginatedResult<Match> searchPublicMatches(
            String query,
            List<Sport> sport,
            Instant startDate,
            Instant endDate,
            EventSort sort,
            int page,
            int pageSize,
            ZoneId timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double latitude,
            Double longitude);

    PaginatedResult<Match> findDashboardMatches(
            User user,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            List<EventStatus> statuses,
            Instant startDate,
            Instant endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            ZoneId timezone,
            List<ParticipantStatus> participantStatuses,
            int page,
            int pageSize);
}
