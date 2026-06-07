package ar.edu.itba.paw.services;

import java.time.LocalDate;
import java.time.LocalTime;

public class TournamentMatchScheduleRequest {

    private final long matchId;
    private final LocalDate startDate;
    private final LocalTime startTime;
    private final LocalDate endDate;
    private final LocalTime endTime;
    private final String address;
    private final Double latitude;
    private final Double longitude;

    public TournamentMatchScheduleRequest(
            final long matchId,
            final LocalDate startDate,
            final LocalTime startTime,
            final LocalDate endDate,
            final LocalTime endTime,
            final String address,
            final Double latitude,
            final Double longitude) {
        this.matchId = matchId;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getMatchId() {
        return matchId;
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

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
