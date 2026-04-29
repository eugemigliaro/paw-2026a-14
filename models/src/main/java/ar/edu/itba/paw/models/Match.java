package ar.edu.itba.paw.models;

import java.math.BigDecimal;
import java.time.Instant;

public class Match {

    private final Long id;
    private final Sport sport;
    private final Long hostUserId;
    private final String address;
    private final String title;
    private final String description;
    private final Instant startsAt;
    private final Instant endsAt;
    private final int maxPlayers;
    private final BigDecimal pricePerPlayer;
    private final String visibility;
    private final String joinPolicy;
    private final String status;
    private final int joinedPlayers;
    private final Long bannerImageId;
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
            final String visibility,
            final String status,
            final int joinedPlayers,
            final Long bannerImageId) {
        this(
                id,
                sport,
                hostUserId,
                address,
                title,
                description,
                startsAt,
                endsAt,
                maxPlayers,
                pricePerPlayer,
                visibility,
                defaultJoinPolicyForVisibility(visibility),
                status,
                joinedPlayers,
                bannerImageId);
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
            final String visibility,
            final String joinPolicy,
            final String status,
            final int joinedPlayers,
            final Long bannerImageId) {
        this(
                id,
                sport,
                hostUserId,
                address,
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
            final String visibility,
            final String joinPolicy,
            final String status,
            final int joinedPlayers,
            final Long bannerImageId,
            final boolean deleted,
            final Instant deletedAt,
            final Long deletedByUserId,
            final String deleteReason) {
        this.id = id;
        this.sport = sport;
        this.hostUserId = hostUserId;
        this.address = address;
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
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedByUserId = deletedByUserId;
        this.deleteReason = deleteReason;
    }

    private static String defaultJoinPolicyForVisibility(final String visibility) {
        if ("public".equalsIgnoreCase(visibility)) {
            return "direct";
        }
        if ("private".equalsIgnoreCase(visibility)) {
            return "invite_only";
        }
        return "approval_required";
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

    public String getVisibility() {
        return visibility;
    }

    public String getJoinPolicy() {
        return joinPolicy;
    }

    public String getStatus() {
        return status;
    }

    public int getJoinedPlayers() {
        return joinedPlayers;
    }

    public Long getBannerImageId() {
        return bannerImageId;
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
}
