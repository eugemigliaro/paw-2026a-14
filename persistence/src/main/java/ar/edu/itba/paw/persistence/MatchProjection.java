package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public record MatchProjection(
        Long id,
        Sport sport,
        Long hostUserId,
        String address,
        Double latitude,
        Double longitude,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        int maxPlayers,
        BigDecimal pricePerPlayer,
        EventVisibility visibility,
        EventJoinPolicy joinPolicy,
        EventStatus status,
        Long joinedPlayers,
        Long bannerImageId,
        Long seriesId,
        Integer seriesOccurrenceIndex,
        Boolean deleted,
        Instant deletedAt,
        Long deletedByUserId,
        String deleteReason) {

    MatchProjection(
            final long id,
            final Sport sport,
            final Long hostUserId,
            final String address,
            final Double latitude,
            final Double longitude,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Long joinedPlayers,
            final Long bannerImageId,
            final Long seriesId,
            final Integer seriesOccurrenceIndex,
            final Boolean deleted,
            final Instant deletedAt,
            final Long deletedByUserId,
            final String deleteReason) {
        this(
                Long.valueOf(id),
                sport,
                hostUserId,
                address,
                latitude,
                longitude,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                visibility,
                joinPolicy,
                status,
                joinedPlayers,
                bannerImageId,
                seriesId,
                seriesOccurrenceIndex,
                deleted,
                deletedAt,
                deletedByUserId,
                deleteReason);
    }

    Match toMatch(final Instant now) {
        final EventStatus effectiveStatus = effectiveStatus(now);
        final int joinedPlayersCount = joinedPlayers == null ? 0 : joinedPlayers.intValue();
        final boolean deletedValue = deleted != null && deleted;

        return new Match(
                id,
                sport,
                hostUserId,
                address,
                latitude,
                longitude,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                visibility,
                joinPolicy,
                effectiveStatus,
                joinedPlayersCount,
                bannerImageId,
                seriesId,
                seriesOccurrenceIndex,
                deletedValue,
                deletedAt,
                deletedByUserId,
                deleteReason);
    }

    private EventStatus effectiveStatus(final Instant now) {
        if (status != EventStatus.OPEN) {
            return status;
        }

        final Instant effectiveEnd = endsAt == null ? startsAt : endsAt;
        if (effectiveEnd == null || now == null || effectiveEnd.isAfter(now)) {
            return status;
        }

        return EventStatus.COMPLETED;
    }
}
