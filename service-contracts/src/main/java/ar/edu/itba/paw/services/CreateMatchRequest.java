package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public class CreateMatchRequest {

    private final Long hostUserId;
    private final String address;
    private final String title;
    private final String description;
    private final Instant startsAt;
    private final Instant endsAt;
    private final int maxPlayers;
    private final BigDecimal pricePerPlayer;
    private final Sport sport;
    private final EventVisibility visibility;
    private final EventJoinPolicy joinPolicy;
    private final EventStatus status;
    private final Long bannerImageId;
    private final CreateRecurrenceRequest recurrence;

    public CreateMatchRequest(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final EventVisibility visibility,
            final EventStatus status,
            final Long bannerImageId) {
        this(
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
                EventVisibility.PUBLIC.equals(visibility)
                        ? EventJoinPolicy.DIRECT
                        : EventJoinPolicy.INVITE_ONLY,
                status,
                bannerImageId,
                null);
    }

    public CreateMatchRequest(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Long bannerImageId) {
        this(
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
                joinPolicy,
                status,
                bannerImageId,
                null);
    }

    public CreateMatchRequest(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final Sport sport,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final Long bannerImageId,
            final CreateRecurrenceRequest recurrence) {
        this.hostUserId = hostUserId;
        this.address = address;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxPlayers = maxPlayers;
        this.pricePerPlayer = pricePerPlayer;
        this.sport = sport;
        this.visibility = visibility;
        this.joinPolicy = joinPolicy;
        this.status = status;
        this.bannerImageId = bannerImageId;
        this.recurrence = recurrence;
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

    public Sport getSport() {
        return sport;
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

    public Long getBannerImageId() {
        return bannerImageId;
    }

    public CreateRecurrenceRequest getRecurrence() {
        return recurrence;
    }

    public boolean isRecurring() {
        return recurrence != null;
    }
}
