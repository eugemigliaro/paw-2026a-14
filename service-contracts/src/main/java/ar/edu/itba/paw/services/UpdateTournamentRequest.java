package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class UpdateTournamentRequest {

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
            final LocalDate startDate,
            final LocalTime startTime,
            final LocalDate endDate,
            final LocalTime endTime,
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
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
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
