package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TournamentDao {

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

    Optional<Tournament> findPublicById(long tournamentId);

    List<Tournament> findPublicRegistrationOrLive(int offset, int limit);

    List<Tournament> findHostedByUser(User host, int offset, int limit);

    Tournament update(Tournament tournament);
}
