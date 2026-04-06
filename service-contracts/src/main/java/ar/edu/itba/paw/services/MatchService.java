package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface MatchService {

    Match createMatch(CreateMatchRequest request);

    default Match createMatch(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final String visibility,
            final String status) {
        return createMatch(
                new CreateMatchRequest(
                        hostUserId,
                        address,
                        title,
                        description,
                        startsAt,
                        endsAt,
                        maxPlayers,
                        pricePerPlayer,
                        sport,
                        visibility,
                        status));
    }

    Optional<Match> findPublicMatchById(Long matchId);

    PaginatedResult<Match> searchPublicMatches(
            String query,
            String sport,
            String time,
            String sort,
            int page,
            int pageSize,
            String timezone);
}
