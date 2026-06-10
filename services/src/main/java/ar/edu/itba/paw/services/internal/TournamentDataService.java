package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TournamentDataService {

    Tournament create(
            User host,
            Sport sport,
            String title,
            String description,
            String address,
            Double latitude,
            Double longitude,
            Instant startsAt,
            Instant endsAt,
            BigDecimal pricePerPlayer,
            ImageMetadata bannerImageMetadata,
            TournamentFormat format,
            int bracketSize,
            int teamSize,
            boolean allowSoloSignup,
            boolean allowTeamDraft,
            Instant registrationOpensAt,
            Instant registrationClosesAt,
            TournamentStatus status);

    Optional<Tournament> findById(long tournamentId);

    void lockForRegistration(long tournamentId);

    Optional<Tournament> findPublicById(long tournamentId);

    List<Tournament> findPublicRegistrationOrLive(int offset, int limit);

    List<Tournament> findPublicTournaments(
            String query,
            List<Sport> sports,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            Double latitude,
            Double longitude,
            int offset,
            int limit);

    int countPublicTournaments(
            String query,
            List<Sport> sports,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    List<Tournament> findDashboardTournaments(
            User host,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            EventSort sort,
            Double latitude,
            Double longitude,
            int offset,
            int limit);

    int countDashboardTournaments(
            User host,
            Boolean upcoming,
            Boolean includeHosted,
            String query,
            List<Sport> sports,
            Instant startsAtFrom,
            Instant startsAtTo,
            BigDecimal minPrice,
            BigDecimal maxPrice);

    Set<Long> findParticipatingTournamentIds(User user, List<Long> tournamentIds);

    Optional<Tournament> refreshScheduleWindow(long tournamentId);

    Tournament update(Tournament tournament);
}
