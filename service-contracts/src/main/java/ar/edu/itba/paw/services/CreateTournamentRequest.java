package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class CreateTournamentRequest {

    private final Sport sport;
    private final String title;
    private final String description;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final LocalDate startDate;
    private final LocalTime startTime;
    private final LocalDate endDate;
    private final LocalTime endTime;
    private final BigDecimal pricePerPlayer;
    private final ImageUpload bannerImage;
    private final TournamentFormat format;
    private final int bracketSize;
    private final int teamSize;
    private final boolean allowSoloSignup;
    private final boolean allowTeamDraft;
    private final LocalDate registrationOpensDate;
    private final LocalTime registrationOpensTime;
    private final LocalDate registrationClosesDate;
    private final LocalTime registrationClosesTime;

    public CreateTournamentRequest(
            final Sport sport,
            final String title,
            final String description,
            final String address,
            final Double latitude,
            final Double longitude,
            final LocalDate startDate,
            final LocalTime startTime,
            final LocalDate endDate,
            final LocalTime endTime,
            final BigDecimal pricePerPlayer,
            final ImageUpload bannerImage,
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
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
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.pricePerPlayer = pricePerPlayer;
        this.bannerImage = bannerImage;
        this.format = format;
        this.bracketSize = bracketSize;
        this.teamSize = teamSize;
        this.allowSoloSignup = allowSoloSignup;
        this.allowTeamDraft = allowTeamDraft;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public LocalTime getEndTime() {
        return endTime;
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
