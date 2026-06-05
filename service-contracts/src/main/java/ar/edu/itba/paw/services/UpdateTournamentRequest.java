package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
    private final ImageUpload bannerImage;
    private final int bracketSize;
    private final int teamSize;
    private final LocalDate registrationOpensDate;
    private final LocalTime registrationOpensTime;
    private final LocalDate registrationClosesDate;
    private final LocalTime registrationClosesTime;

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
            final ImageUpload bannerImage,
            final int bracketSize,
            final int teamSize,
            final LocalDate registrationOpensDate,
            final LocalTime registrationOpensTime,
            final LocalDate registrationClosesDate,
            final LocalTime registrationClosesTime) {
        this.sport = sport;
        this.title = title;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.pricePerPlayer = pricePerPlayer;
        this.bannerImage = bannerImage;
        this.bracketSize = bracketSize;
        this.teamSize = teamSize;
        this.registrationOpensDate = registrationOpensDate;
        this.registrationOpensTime = registrationOpensTime;
        this.registrationClosesDate = registrationClosesDate;
        this.registrationClosesTime = registrationClosesTime;
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

    public ImageUpload getBannerImage() {
        return bannerImage;
    }

    public int getBracketSize() {
        return bracketSize;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public LocalDate getRegistrationOpensDate() {
        return registrationOpensDate;
    }

    public LocalTime getRegistrationOpensTime() {
        return registrationOpensTime;
    }

    public LocalDate getRegistrationClosesDate() {
        return registrationClosesDate;
    }

    public LocalTime getRegistrationClosesTime() {
        return registrationClosesTime;
    }
}
