package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.EventJoinPolicyConverter;
import ar.edu.itba.paw.models.converters.EventStatusConverter;
import ar.edu.itba.paw.models.converters.EventVisibilityConverter;
import ar.edu.itba.paw.models.converters.SportConverter;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "matches_matchid_seq")
    @SequenceGenerator(
            sequenceName = "matches_matchid_seq",
            name = "matches_matchid_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "sport", length = 30, nullable = false)
    @Convert(converter = SportConverter.class)
    private Sport sport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "max_players", nullable = false)
    private int maxPlayers;

    @Column(name = "price_per_player")
    private BigDecimal pricePerPlayer;

    @Column(name = "visibility", length = 20, nullable = false)
    @Convert(converter = EventVisibilityConverter.class)
    private EventVisibility visibility;

    @Column(name = "join_policy", length = 30, nullable = false)
    @Convert(converter = EventJoinPolicyConverter.class)
    private EventJoinPolicy joinPolicy;

    @Column(name = "status", length = 20, nullable = false)
    @Convert(converter = EventStatusConverter.class)
    private EventStatus status;

    @Transient private int joinedPlayers;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_image_id")
    private ImageMetadata bannerImageMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id")
    private MatchSeries series;

    @Column(name = "series_occurrence_index")
    private Integer seriesOccurrenceIndex;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_user_id")
    private User deletedByUser;

    @Column(name = "delete_reason")
    private String deleteReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "match")
    private List<MatchParticipant> participants;

    Match() {}

    public Match(
            final Long id,
            final Sport sport,
            final User host,
            final String address,
            final Double latitude,
            final Double longitude,
            final String title,
            final String description,
            final Instant startsAt,
            final Instant endsAt,
            final int maxPlayers,
            final BigDecimal pricePerPlayer,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final EventStatus status,
            final int joinedPlayers,
            final ImageMetadata bannerImageMetadata,
            final MatchSeries series,
            final Integer seriesOccurrenceIndex,
            final boolean deleted,
            final Instant deletedAt,
            final User deletedByUser,
            final String deleteReason) {
        this.id = id;
        this.sport = sport;
        this.host = host;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.maxPlayers = maxPlayers;
        this.pricePerPlayer = pricePerPlayer;
        this.visibility = visibility;
        this.joinPolicy = joinPolicy;
        this.status = status;
        this.joinedPlayers = joinedPlayers;
        this.bannerImageMetadata = bannerImageMetadata;
        this.series = series;
        this.seriesOccurrenceIndex = seriesOccurrenceIndex;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.deletedByUser = deletedByUser;
        this.deleteReason = deleteReason;
    }

    public Long getId() {
        return id;
    }

    public Sport getSport() {
        return sport;
    }

    public User getHost() {
        return host;
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

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
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

    public EventVisibility getVisibility() {
        return visibility;
    }

    public EventJoinPolicy getJoinPolicy() {
        return joinPolicy;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getJoinedPlayers() {
        return joinedPlayers;
    }

    public void setJoinedPlayers(final int joinedPlayers) {
        this.joinedPlayers = joinedPlayers;
    }

    public ImageMetadata getBannerImageMetadata() {
        return bannerImageMetadata;
    }

    public MatchSeries getSeries() {
        return series;
    }

    public Integer getSeriesOccurrenceIndex() {
        return seriesOccurrenceIndex;
    }

    public boolean isRecurringOccurrence() {
        return getSeries() != null;
    }

    public boolean hasBannerImage() {
        return bannerImageMetadata != null;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public User getDeletedByUser() {
        return deletedByUser;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public int getAvailableSpots() {
        return Math.max(maxPlayers - joinedPlayers, 0);
    }

    public List<MatchParticipant> getParticipants() {
        return participants;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setStartsAt(final Instant startsAt) {
        this.startsAt = startsAt;
    }

    public void setEndsAt(final Instant endsAt) {
        this.endsAt = endsAt;
    }

    public void setMaxPlayers(final int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public void setPricePerPlayer(final BigDecimal pricePerPlayer) {
        this.pricePerPlayer = pricePerPlayer;
    }

    public void setSport(final Sport sport) {
        this.sport = sport;
    }

    public void setHost(final User host) {
        this.host = host;
    }

    public void setVisibility(final EventVisibility visibility) {
        this.visibility = visibility;
    }

    public void setJoinPolicy(final EventJoinPolicy joinPolicy) {
        this.joinPolicy = joinPolicy;
    }

    public void setStatus(final EventStatus status) {
        this.status = status;
    }

    public void setBannerImageMetadata(final ImageMetadata bannerImageMetadata) {
        this.bannerImageMetadata = bannerImageMetadata;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public void setSeries(final MatchSeries series) {
        this.series = series;
    }

    public void setSeriesOccurrenceIndex(final Integer seriesOccurrenceIndex) {
        this.seriesOccurrenceIndex = seriesOccurrenceIndex;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(final Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setDeletedBy(final User deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public void setDeleteReason(final String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Match{"
                + "id="
                + id
                + ", sport="
                + sport
                + ", hostUserId="
                + (host == null ? null : host.getId())
                + ", startsAt="
                + startsAt
                + ", endsAt="
                + endsAt
                + ", maxPlayers="
                + maxPlayers
                + ", joinedPlayers="
                + joinedPlayers
                + ", availableSpots="
                + getAvailableSpots()
                + ", pricePerPlayer="
                + pricePerPlayer
                + ", visibility="
                + visibility
                + ", joinPolicy="
                + joinPolicy
                + ", status="
                + status
                + ", bannerImageId="
                + (bannerImageMetadata == null ? null : bannerImageMetadata.getId())
                + ", seriesId="
                + (series == null ? null : series.getId())
                + ", seriesOccurrenceIndex="
                + seriesOccurrenceIndex
                + ", deleted="
                + deleted
                + ", deletedAt="
                + deletedAt
                + ", deletedByUserId="
                + (deletedByUser == null ? null : deletedByUser.getId())
                + '}';
    }
}
