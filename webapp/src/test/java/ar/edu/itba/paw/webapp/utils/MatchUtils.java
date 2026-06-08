package ar.edu.itba.paw.webapp.utils;

import ar.edu.itba.paw.models.ImageMetadata;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.RecurrenceFrequency;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;

public class MatchUtils {

    public static Builder match(final Long id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final Long id;
        private Sport sport = Sport.PADEL;
        private User host = UserUtils.getUser(7L);
        private String address = "Address";
        private Double latitude = null;
        private Double longitude = null;
        private String title = "Title";
        private String description = "Desc";
        private Instant startsAt = Instant.parse("2026-04-06T10:00:00Z");
        private Instant endsAt = Instant.parse("2026-04-06T12:00:00Z");
        private int maxPlayers = 8;
        private BigDecimal pricePerPlayer = null;
        private EventVisibility visibility = EventVisibility.PUBLIC;
        private EventJoinPolicy joinPolicy = EventJoinPolicy.DIRECT;
        private EventStatus status = EventStatus.OPEN;
        private int joinedPlayers = 0;
        private ImageMetadata bannerImageMetadata = null;
        private MatchSeries series = null;
        private Integer seriesOccurrenceIndex = null;
        private boolean deleted = false;
        private Instant deletedAt = null;
        private User deletedByUser = null;
        private String deleteReason = null;

        private Builder(final Long id) {
            this.id = id;
        }

        public Builder sport(final Sport sport) {
            this.sport = sport;
            return this;
        }

        public Builder host(final User host) {
            this.host = host;
            return this;
        }

        public Builder hostId(final Long hostId) {
            this.host = UserUtils.getUser(hostId);
            return this;
        }

        public Builder address(final String address) {
            this.address = address;
            return this;
        }

        public Builder coords(final Double latitude, final Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder latitude(final Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(final Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder title(final String title) {
            this.title = title;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public Builder startsAt(final Instant startsAt) {
            this.startsAt = startsAt;
            return this;
        }

        public Builder endsAt(final Instant endsAt) {
            this.endsAt = endsAt;
            return this;
        }

        public Builder maxPlayers(final int maxPlayers) {
            this.maxPlayers = maxPlayers;
            return this;
        }

        public Builder price(final BigDecimal pricePerPlayer) {
            this.pricePerPlayer = pricePerPlayer;
            return this;
        }

        public Builder visibility(final EventVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        public Builder joinPolicy(final EventJoinPolicy joinPolicy) {
            this.joinPolicy = joinPolicy;
            return this;
        }

        public Builder status(final EventStatus status) {
            this.status = status;
            return this;
        }

        public Builder joinedPlayers(final int joinedPlayers) {
            this.joinedPlayers = joinedPlayers;
            return this;
        }

        public Builder banner(final ImageMetadata bannerImageMetadata) {
            this.bannerImageMetadata = bannerImageMetadata;
            return this;
        }

        public Builder series(final MatchSeries series) {
            this.series = series;
            return this;
        }

        public Builder seriesOccurrenceIndex(final Integer seriesOccurrenceIndex) {
            this.seriesOccurrenceIndex = seriesOccurrenceIndex;
            return this;
        }

        public Builder deleted(final boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder deletedAt(final Instant deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Builder deletedByUser(final User deletedByUser) {
            this.deletedByUser = deletedByUser;
            return this;
        }

        public Builder deleteReason(final String deleteReason) {
            this.deleteReason = deleteReason;
            return this;
        }

        public Match build() {
            return new Match(
                    id,
                    sport,
                    host,
                    address,
                    latitude,
                    longitude,
                    title,
                    description,
                    startsAt,
                    endsAt,
                    maxPlayers,
                    pricePerPlayer,
                    visibility,
                    joinPolicy,
                    status,
                    joinedPlayers,
                    bannerImageMetadata,
                    series,
                    seriesOccurrenceIndex,
                    deleted,
                    deletedAt,
                    deletedByUser,
                    deleteReason);
        }
    }

    public static Match createMatchWithId(
            Long matchId, Long hostId, Sport sport, Instant startsAt, Integer maxPlayers) {
        return new Match(
                matchId,
                sport,
                UserUtils.getUser(hostId),
                "Address",
                null,
                null,
                "Title",
                "Desc",
                startsAt,
                startsAt.plusSeconds(3600),
                maxPlayers,
                null,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                0,
                null,
                null,
                null,
                false,
                null,
                null,
                null);
    }

    public static MatchSeries getMatchSeries(Long seriesId, User host) {
        return new MatchSeries(
                seriesId,
                host,
                RecurrenceFrequency.WEEKLY,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
