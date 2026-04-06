package ar.edu.itba.paw.services;

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
    private final String visibility;
    private final String status;

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
            final String visibility,
            final String status) {
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
        this.status = status;
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

    public String getVisibility() {
        return visibility;
    }

    public String getStatus() {
        return status;
    }
}
