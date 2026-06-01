package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TournamentService {

    Tournament createTournament(User host, CreateTournamentRequest request);

    Optional<Tournament> findPublicTournament(long tournamentId);

    Optional<Tournament> findTournamentForHost(long tournamentId, User host);

    @Deprecated(forRemoval = true)
    PaginatedResult<Tournament> searchPublicTournaments(
            String query,
            String sports,
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

    PaginatedResult<Tournament> searchPublicTournaments(
            String query,
            List<Sport> sport,
            Instant startDate,
            Instant endDate,
            String sort,
            int page,
            int pageSize,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Double latitude,
            Double longitude);

    PaginatedResult<Tournament> findHostedTournaments(
            User host,
            String query,
            List<Sport> sport,
            Instant startDate,
            Instant endDate,
            String sort,
            int page,
            int pageSize,
            String timezone,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    Tournament update(long tournamentId, User actingUser, UpdateTournamentRequest request);

    Tournament cancel(long tournamentId, User actingUser, String reason);
}
