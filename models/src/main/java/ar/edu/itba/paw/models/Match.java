package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public class Match {

    private final Long id;
    private final Sport sport;
    private final Long hostUserId;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String title;
    private final String description;
    private final Instant startsAt;
    private final Instant endsAt;
    private final int maxPlayers;
    private final BigDecimal pricePerPlayer;
    private final EventVisibility visibility;
    private final EventJoinPolicy joinPolicy;
    private final EventStatus status;
    private final int joinedPlayers;
    private final Long bannerImageId;
    private final Long seriesId;
    private final Integer seriesOccurrenceIndex;
    private final boolean deleted;
    private final Instant deletedAt;
    private final Long deletedByUserId;
    private final String deleteReason;

    public Match(
            final Long id,
            final Sport sport,
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final int joinedPlayers,
            final Long bannerImageId) {
        this(
                id,
                sport,
                hostUserId,
                address,
                null,
                null,
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
                null,
                null,
                false,
                null,
                null,
                null);
    }

    public Match(
            final Long id,
            final Sport sport,
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final int joinedPlayers,
            final Long bannerImageId,
            final Long seriesId,
            final Integer seriesOccurrenceIndex) {
        this(
                id,
                sport,
                hostUserId,
                address,
                null,
                null,
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
                false,
                null,
                null,
                null);
    }

    public Match(
            final Long id,
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
            final int joinedPlayers,
            final Long bannerImageId,
            final Long seriesId,
            final Integer seriesOccurrenceIndex) {
        this(
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
                status,
                joinedPlayers,
                bannerImageId,
                seriesId,
                seriesOccurrenceIndex,
                false,
                null,
                null,
                null);
    }

    public Match(
            final Long id,
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
            final int joinedPlayers,
            final Long bannerImageId,
            final Long seriesId,
            final Integer seriesOccurrenceIndex,
            final boolean deleted,
            final Instant deletedAt,
            final Long deletedByUserId,
            final String deleteReason) {
        this.id = id;
        this.sport = sport;
        this.hostUserId = hostUserId;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxPlayers = maxPlayers;
        this.pricePerPlayer = pricePerPlayer;
        this.visibility = visibility;
        this.joinPolicy = joinPolicy;
        this.status = status;
        this.joinedPlayers = joinedPlayers;
        this.bannerImageId = bannerImageId;
        this.seriesId = seriesId;
        this.seriesOccurrenceIndex = seriesOccurrenceIndex;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedByUserId = deletedByUserId;
        this.deleteReason = deleteReason;
    }

    public Long getId() {
        return id;
    }

    public Sport getSport() {
        return sport;
    }

    public Long getHostUserId() {
        return hostUserId;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public BigDecimal getPricePerPlayer() {
        return pricePerPlayer;
    }

    public EventVisibility getVisibility() {
        return visibility;
    }

    public EventJoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getJoinedPlayers() {
        return joinedPlayers;
    }

    public Long getBannerImageId() {
        return bannerImageId;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public Integer getSeriesOccurrenceIndex() {
        return seriesOccurrenceIndex;
    }

    public boolean isRecurringOccurrence() {
        return seriesId != null;
    }

    public boolean hasBannerImage() {
        return bannerImageId != null;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Long getDeletedByUserId() {
        return deletedByUserId;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public int getAvailableSpots() {
        return Math.max(maxPlayers - joinedPlayers, 0);
    }

    @Override
    public String toString() {
        return "Match{"
                + "id="
                + id
                + ", sport="
                + sport
                + ", hostUserId="
                + hostUserId
                + ", startsAt="
                + startsAt
                + ", endsAt="
                + endsAt
                + ", maxPlayers="
                + maxPlayers
                + ", joinedPlayers="
                + joinedPlayers
                + ", availableSpots="
                + getAvailableSpots()
                + ", pricePerPlayer="
                + pricePerPlayer
                + ", visibility="
                + visibility
                + ", joinPolicy="
                + joinPolicy
                + ", status="
                + status
                + ", bannerImageId="
                + bannerImageId
                + ", seriesId="
                + seriesId
                + ", seriesOccurrenceIndex="
                + seriesOccurrenceIndex
                + ", deleted="
                + deleted
                + ", deletedAt="
                + deletedAt
                + ", deletedByUserId="
                + deletedByUserId
                + '}';
    }
}
