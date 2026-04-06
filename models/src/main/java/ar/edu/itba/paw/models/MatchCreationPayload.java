package ar.edu.itba.paw.models;

import java.math.BigDecimal;

public class MatchCreationPayload {

    private Long hostUserId;
    private String address;
    private String title;
    private String description;
    private Long startsAtEpochMillis;
    private Long endsAtEpochMillis;
    private int maxPlayers;
    private BigDecimal pricePerPlayer;
    private String sport;
    private String visibility;
    private String status;

    public MatchCreationPayload() {}

    public MatchCreationPayload(
            final Long hostUserId,
            final String address,
            final String title,
            final String description,
            final Long startsAtEpochMillis,
            final Long endsAtEpochMillis,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final String sport,
            final String visibility,
            final String status) {
        this.hostUserId = hostUserId;
        this.address = address;
        this.title = title;
        this.description = description;
        this.startsAtEpochMillis = startsAtEpochMillis;
        this.endsAtEpochMillis = endsAtEpochMillis;
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

    public Long getStartsAtEpochMillis() {
        return startsAtEpochMillis;
    }

    public Long getEndsAtEpochMillis() {
        return endsAtEpochMillis;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public BigDecimal getPricePerPlayer() {
        return pricePerPlayer;
    }

    public String getSport() {
        return sport;
    }

    public String getVisibility() {
        return visibility;
    }

    public String getStatus() {
        return status;
    }
}
