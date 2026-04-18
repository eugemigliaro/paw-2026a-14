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

    List<User> findConfirmedParticipants(Long matchId);

    PaginatedResult<Match> findHostedMatches(
            Long hostUserId,
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
            Long userId,
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
            String query,
            String sport,
            String startDate,
            String endDate,
            String sort,
            int page,
            int pageSize,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice);
}
