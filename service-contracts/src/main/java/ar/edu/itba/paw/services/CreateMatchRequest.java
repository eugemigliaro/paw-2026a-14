package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public class CreateMatchRequest {

    private final User host;
    private final String address;
    private final Double latitude;
    private final Double longitude;
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
    private final ImageMetadata bannerImageMetadata;
    private final CreateRecurrenceRequest recurrence;

    public CreateMatchRequest(
            final User host,
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
            final ImageMetadata bannerImageMetadata) {
        this(
                host,
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
                bannerImageMetadata,
                null);
    }

    public CreateMatchRequest(
            final User host,
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
            final ImageMetadata bannerImageMetadata) {
        this(
                host,
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
                bannerImageMetadata,
                null);
    }

    public CreateMatchRequest(
            final User host,
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
            final ImageMetadata bannerImageMetadata,
            final CreateRecurrenceRequest recurrence) {
        this(
                host,
                address,
                null,
                null,
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
                bannerImageMetadata,
                recurrence);
    }

    public CreateMatchRequest(
            final User host,
            final String address,
            final Double latitude,
            final Double longitude,
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
            final ImageMetadata bannerImageMetadata,
            final CreateRecurrenceRequest recurrence) {
        this.host = host;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
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
        this.bannerImageMetadata = bannerImageMetadata;
        this.recurrence = recurrence;
    }

    public User getHost() {
        return host;
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

    public ImageMetadata getBannerImageMetadata() {
        return bannerImageMetadata;
    }

    public CreateRecurrenceRequest getRecurrence() {
        return recurrence;
    }

    public boolean isRecurring() {
        return recurrence != null;
    }
}
