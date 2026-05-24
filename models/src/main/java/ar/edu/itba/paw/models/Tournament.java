package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.SportConverter;
import ar.edu.itba.paw.models.converters.TournamentFormatConverter;
import ar.edu.itba.paw.models.converters.TournamentPairingStrategyConverter;
import ar.edu.itba.paw.models.converters.TournamentStatusConverter;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentPairingStrategy;
import ar.edu.itba.paw.models.types.TournamentStatus;
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
import javax.persistence.Version;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tournaments_id_seq")
    @SequenceGenerator(
            sequenceName = "tournaments_id_seq",
            name = "tournaments_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    @Column(name = "sport", length = 30, nullable = false)
    @Convert(converter = SportConverter.class)
    private Sport sport;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "price_per_player")
    private BigDecimal pricePerPlayer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banner_image_id")
    private ImageMetadata bannerImageMetadata;

    @Column(name = "format", length = 40, nullable = false)
    @Convert(converter = TournamentFormatConverter.class)
    private TournamentFormat format;

    @Column(name = "bracket_size", nullable = false)
    private Short bracketSize;

    @Column(name = "team_size", nullable = false)
    private Short teamSize;

    @Column(name = "allow_solo_signup", nullable = false)
    private boolean allowSoloSignup;

    @Column(name = "allow_team_draft", nullable = false)
    private boolean allowTeamDraft;

    @Column(name = "registration_opens_at", nullable = false)
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at", nullable = false)
    private Instant registrationClosesAt;

    @Column(name = "status", length = 40, nullable = false)
    @Convert(converter = TournamentStatusConverter.class)
    private TournamentStatus status;

    @Column(name = "pairing_strategy", length = 30)
    @Convert(converter = TournamentPairingStrategyConverter.class)
    private TournamentPairingStrategy pairingStrategy;

    @Column(name = "registration_closed_at")
    private Instant registrationClosedAt;

    @Column(name = "bracket_generated_at")
    private Instant bracketGeneratedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tournament")
    private List<TournamentTeam> teams;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tournament")
    private List<TournamentSoloEntry> soloEntries;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "tournament")
    private List<TournamentMatch> matches;

    Tournament() {}

    public Tournament(
            final Long id,
            final User host,
            final Sport sport,
            final String title,
            final String description,
            final String address,
            final Double latitude,
            final Double longitude,
            final Instant startsAt,
            final Instant endsAt,
            final BigDecimal pricePerPlayer,
            final ImageMetadata bannerImageMetadata,
            final TournamentFormat format,
            final int bracketSize,
            final int teamSize,
            final boolean allowSoloSignup,
            final boolean allowTeamDraft,
            final Instant registrationOpensAt,
            final Instant registrationClosesAt,
            final TournamentStatus status,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.host = host;
        this.sport = sport;
        this.title = title;
        this.description = description;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.pricePerPlayer = pricePerPlayer;
        this.bannerImageMetadata = bannerImageMetadata;
        this.format = format;
        this.bracketSize = (short) bracketSize;
        this.teamSize = (short) teamSize;
        this.allowSoloSignup = allowSoloSignup;
        this.allowTeamDraft = allowTeamDraft;
        this.registrationOpensAt = registrationOpensAt;
        this.registrationClosesAt = registrationClosesAt;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getHost() {
        return host;
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

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
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

    public ImageMetadata getBannerImageMetadata() {
        return bannerImageMetadata;
    }

    public boolean hasBannerImage() {
        return bannerImageMetadata != null;
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

    public TournamentStatus getStatus() {
        return status;
    }

    public TournamentPairingStrategy getPairingStrategy() {
        return pairingStrategy;
    }

    public Instant getRegistrationClosedAt() {
        return registrationClosedAt;
    }

    public Instant getBracketGeneratedAt() {
        return bracketGeneratedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<TournamentTeam> getTeams() {
        return teams;
    }

    public List<TournamentSoloEntry> getSoloEntries() {
        return soloEntries;
    }

    public List<TournamentMatch> getMatches() {
        return matches;
    }

    public void setHost(final User host) {
        this.host = host;
    }

    public void setSport(final Sport sport) {
        this.sport = sport;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public void setLatitude(final Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(final Double longitude) {
        this.longitude = longitude;
    }

    public void setStartsAt(final Instant startsAt) {
        this.startsAt = startsAt;
    }

    public void setEndsAt(final Instant endsAt) {
        this.endsAt = endsAt;
    }

    public void setPricePerPlayer(final BigDecimal pricePerPlayer) {
        this.pricePerPlayer = pricePerPlayer;
    }

    public void setBannerImageMetadata(final ImageMetadata bannerImageMetadata) {
        this.bannerImageMetadata = bannerImageMetadata;
    }

    public void setStatus(final TournamentStatus status) {
        this.status = status;
    }

    public void setPairingStrategy(final TournamentPairingStrategy pairingStrategy) {
        this.pairingStrategy = pairingStrategy;
    }

    public void setRegistrationClosedAt(final Instant registrationClosedAt) {
        this.registrationClosedAt = registrationClosedAt;
    }

    public void setBracketGeneratedAt(final Instant bracketGeneratedAt) {
        this.bracketGeneratedAt = bracketGeneratedAt;
    }

    public void setStartedAt(final Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void setCompletedAt(final Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void setCancelledAt(final Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public void setCancelReason(final String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public void setDeletedAt(final Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void setDeletedByUser(final User deletedByUser) {
        this.deletedByUser = deletedByUser;
    }

    public void setDeleteReason(final String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Tournament{"
                + "id="
                + id
                + ", hostUserId="
                + (host == null ? null : host.getId())
                + ", sport="
                + sport
                + ", title='"
                + title
                + '\''
                + ", bracketSize="
                + bracketSize
                + ", teamSize="
                + teamSize
                + ", status="
                + status
                + ", deleted="
                + deleted
                + '}';
    }
}
