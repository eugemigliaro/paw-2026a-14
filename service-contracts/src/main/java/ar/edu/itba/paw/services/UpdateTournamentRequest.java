package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public class UpdateTournamentRequest {

    private final Sport sport;
    private final String title;
    private final String description;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final Instant startsAt;
    private final Instant endsAt;
    private final BigDecimal pricePerPlayer;
    private final ImageMetadata bannerImageMetadata;
    private final Instant registrationOpensAt;
    private final Instant registrationClosesAt;

    public UpdateTournamentRequest(
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
            final Instant registrationOpensAt,
            final Instant registrationClosesAt) {
        this.sport = sport;
        this.title = title;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.pricePerPlayer = pricePerPlayer;
        this.bannerImageMetadata = bannerImageMetadata;
        this.registrationOpensAt = registrationOpensAt;
        this.registrationClosesAt = registrationClosesAt;
    }

    public Sport getSport() {
        return sport;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BigDecimal getPricePerPlayer() {
        return pricePerPlayer;
    }

    public ImageMetadata getBannerImageMetadata() {
        return bannerImageMetadata;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }
}
