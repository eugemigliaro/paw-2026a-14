package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.util.Optional;

public interface TournamentService {

    Tournament createTournament(User host, CreateTournamentRequest request);

    Optional<Tournament> findPublicTournament(long tournamentId);

    Optional<Tournament> findTournamentForHost(long tournamentId, User host);

    PaginatedResult<Tournament> searchPublicTournaments(
            String query,
            String sport,
            String startDate,
            String endDate,
            String sort,
            int page,
            int pageSize,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double latitude,
            Double longitude);

    default PaginatedResult<Tournament> searchPublicTournaments(
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
        return searchPublicTournaments(
                query, sport, startDate, endDate, sort, page, pageSize, timezone, minPrice,
                maxPrice, null, null);
    }

    Tournament update(long tournamentId, User actingUser, UpdateTournamentRequest request);

    Tournament cancel(long tournamentId, User actingUser, String reason);
}
