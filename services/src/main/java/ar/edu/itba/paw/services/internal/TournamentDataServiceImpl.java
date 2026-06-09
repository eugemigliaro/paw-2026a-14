package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventSort;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.persistence.TournamentDao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TournamentDataServiceImpl implements TournamentDataService {

    private final TournamentDao tournamentDao;

    public TournamentDataServiceImpl(final TournamentDao tournamentDao) {
        this.tournamentDao = Objects.requireNonNull(tournamentDao);
    }

    @Override
    public Tournament create(
            final User host,
            final Sport sport,
            final String title,
            final String description,
            final String address,
            final Double latitude,
            final Double longitude,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal pricePerPlayer,
            final ImageMetadata bannerImageMetadata,
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt,
            final TournamentStatus status) {
        return tournamentDao.create(
                host,
                sport,
                title,
                description,
                address,
                latitude,
                longitude,
                startsAt,
                endsAt,
                pricePerPlayer,
                bannerImageMetadata,
                format,
                bracketSize,
                teamSize,
                allowSoloSignup,
                allowTeamDraft,
                registrationOpensAt,
                registrationClosesAt,
                status);
    }

    @Override
    public Optional<Tournament> findById(final long tournamentId) {
        return tournamentDao.findById(tournamentId);
    }

    @Override
    public Optional<Tournament> findPublicById(final long tournamentId) {
        return tournamentDao.findPublicById(tournamentId);
    }

    @Override
    public List<Tournament> findPublicRegistrationOrLive(final int offset, final int limit) {
        return tournamentDao.findPublicRegistrationOrLive(offset, limit);
    }

    @Override
    public List<Tournament> findPublicTournaments(
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        return tournamentDao.findPublicTournaments(
                query,
                sports,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                latitude,
                longitude,
                offset,
                limit);
    }

    @Override
    public int countPublicTournaments(
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return tournamentDao.countPublicTournaments(
                query, sports, startsAtFrom, startsAtTo, minPrice, maxPrice);
    }

    @Override
    public List<Tournament> findDashboardTournaments(
            final User host,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice,
            final EventSort sort,
            final Double latitude,
            final Double longitude,
            final int offset,
            final int limit) {
        return tournamentDao.findDashboardTournaments(
                host,
                upcoming,
                includeHosted,
                query,
                sports,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice,
                sort,
                latitude,
                longitude,
                offset,
                limit);
    }

    @Override
    public int countDashboardTournaments(
            final User host,
            final Boolean upcoming,
            final Boolean includeHosted,
            final String query,
            final List<Sport> sports,
            final Instant startsAtFrom,
            final Instant startsAtTo,
            final BigDecimal minPrice,
            final BigDecimal maxPrice) {
        return tournamentDao.countDashboardTournaments(
                host,
                upcoming,
                includeHosted,
                query,
                sports,
                startsAtFrom,
                startsAtTo,
                minPrice,
                maxPrice);
    }

    @Override
    public Set<Long> findParticipatingTournamentIds(
            final User user, final List<Long> tournamentIds) {
        return tournamentDao.findParticipatingTournamentIds(user, tournamentIds);
    }

    @Override
    public Optional<Tournament> refreshScheduleWindow(final long tournamentId) {
        return tournamentDao.refreshScheduleWindow(tournamentId);
    }

    @Override
    public Tournament update(final Tournament tournament) {
        return tournamentDao.update(tournament);
    }
}
