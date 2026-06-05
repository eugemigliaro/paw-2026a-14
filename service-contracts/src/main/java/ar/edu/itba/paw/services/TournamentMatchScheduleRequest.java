package ar.edu.itba.paw.services;

import java.time.Instant;

public class TournamentMatchScheduleRequest {

    private final long matchId;
    private final Instant startsAt;
    private final Instant endsAt;
    private final String address;
    private final Double latitude;
    private final Double longitude;

    public TournamentMatchScheduleRequest(
            final long matchId,
            final Instant startsAt,
            final Instant endsAt,
            final String address,
            final Double latitude,
            final Double longitude) {
        this.matchId = matchId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getMatchId() {
        return matchId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
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
