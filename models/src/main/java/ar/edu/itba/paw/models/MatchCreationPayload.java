package ar.edu.itba.paw.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class MatchCreationPayload {

    private final Long hostUserId;
    private final String address;
    private final String title;
    private final String description;
    private final Long startsAtEpochMillis;
    private final Long endsAtEpochMillis;
    private final int maxPlayers;
    private final BigDecimal pricePerPlayer;
    private final String sport;
    private final String visibility;
    private final String status;
    private final Long bannerImageId;

    @JsonCreator
    public MatchCreationPayload(
            @JsonProperty("hostUserId") final Long hostUserId,
            @JsonProperty("address") final String address,
            @JsonProperty("title") final String title,
            @JsonProperty("description") final String description,
            @JsonProperty("startsAtEpochMillis") final Long startsAtEpochMillis,
            @JsonProperty("endsAtEpochMillis") final Long endsAtEpochMillis,
            @JsonProperty("maxPlayers") final int maxPlayers,
            @JsonProperty("pricePerPlayer") final BigDecimal pricePerPlayer,
            @JsonProperty("sport") final String sport,
            @JsonProperty("visibility") final String visibility,
            @JsonProperty("status") final String status,
            @JsonProperty("bannerImageId") final Long bannerImageId) {
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
        this.bannerImageId = bannerImageId;
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

    public Long getBannerImageId() {
        return bannerImageId;
    }
}
