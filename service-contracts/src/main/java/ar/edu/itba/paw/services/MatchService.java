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

    List<User> findConfirmedParticipants(Long matchId);

    Match updateMatch(Long matchId, Long actingUserId, UpdateMatchRequest request);

    PaginatedResult<Match> searchPublicMatches(
            String query,
            String sport,
            String time,
            String sort,
            int page,
            int pageSize,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    default PaginatedResult<Match> searchPublicMatches(
            String query,
            String sport,
            String time,
            String sort,
            int page,
            int pageSize,
            String timezone) {
        return searchPublicMatches(query, sport, time, sort, page, pageSize, timezone, null, null);
    }
}
