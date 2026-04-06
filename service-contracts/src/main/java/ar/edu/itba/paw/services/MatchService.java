package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MatchService {

    Match createMatch(
            Long hostUserId,
            String address,
            String title,
            String description,
            Instant startsAt,
            Instant endsAt,
            int maxPlayers,
            BigDecimal pricePerPlayer,
            Sport sport,
            String visibility,
            String status);

    Optional<Match> findPublicMatchById(Long matchId);

    List<User> findConfirmedParticipants(Long matchId);

    PaginatedResult<Match> searchPublicMatches(
            String query,
            String sport,
            String time,
            String sort,
            int page,
            int pageSize,
            String timezone);
}
