package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MatchService {

    Match createMatch(CreateMatchRequest request);

    Optional<Match> findMatchById(Long matchId);

    Optional<Match> findPublicMatchById(Long matchId);

    List<Match> findSeriesOccurrences(Long seriesId);

    List<User> findConfirmedParticipants(Long matchId);

    Match updateMatch(Long matchId, User actingUser, UpdateMatchRequest request);

    List<Match> updateSeriesFromOccurrence(
            Long matchId, User actingUser, UpdateMatchRequest request);

    Match cancelMatch(Long matchId, User actingUser);

    List<Match> cancelSeriesFromOccurrence(Long matchId, User actingUser);

    PaginatedResult<Match> findHostedMatches(
            User hostUser,
            Boolean upcoming,
            String query,
            String sport,
            String visibility,
            String status,
            String startDate,
            String endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            String timezone,
            int page,
            int pageSize);

    PaginatedResult<Match> findJoinedMatches(
            User user,
            Boolean upcoming,
            String query,
            String sport,
            String visibility,
            String status,
            String startDate,
            String endDate,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sort,
            String timezone,
            int page,
            int pageSize);

    PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final Double latitude,
            final Double longitude);

    default PaginatedResult<Match> searchPublicMatches(
            final String query,
            final String sport,
            final String startDate,
            final String endDate,
            final String sort,
            final int page,
            final int pageSize,
            final String timezone,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return searchPublicMatches(
                query, sport, startDate, endDate, sort, page, pageSize, timezone, minPrice,
                maxPrice, null, null);
    }
}
