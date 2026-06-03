package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import java.math.BigDecimal;
import java.time.Instant;

public class CreateTournamentRequest {

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
    private final TournamentFormat format;
    private final int bracketSize;
    private final int teamSize;
    private final boolean allowSoloSignup;
    private final boolean allowTeamDraft;
    private final Instant registrationOpensAt;
    private final Instant registrationClosesAt;

    public CreateTournamentRequest(
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
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
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
        this.bannerImage = bannerImage;
        this.format = format;
        this.bracketSize = bracketSize;
        this.teamSize = teamSize;
        this.allowSoloSignup = allowSoloSignup;
        this.allowTeamDraft = allowTeamDraft;
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

    public ImageUpload getBannerImage() {
        return bannerImage;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public int getBracketSize() {
        return bracketSize;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public boolean isAllowSoloSignup() {
        return allowSoloSignup;
    }

    public boolean isAllowTeamDraft() {
        return allowTeamDraft;
    }

    public Instant getRegistrationOpensAt() {
        return registrationOpensAt;
    }

    public Instant getRegistrationClosesAt() {
        return registrationClosesAt;
    }
}
