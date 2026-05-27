package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.TournamentMatchStatusConverter;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "tournament_matches",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"tournament_id", "round_number", "match_index"})
        })
public class TournamentMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tournament_matches_id_seq")
    @SequenceGenerator(
            sequenceName = "tournament_matches_id_seq",
            name = "tournament_matches_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "round_number", nullable = false)
    private Short roundNumber;

    @Column(name = "match_index", nullable = false)
    private Short matchIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private TournamentTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private TournamentTeam teamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private TournamentTeam winnerTeam;

    @Column(name = "scheduled_starts_at")
    private Instant scheduledStartsAt;

    @Column(name = "scheduled_ends_at")
    private Instant scheduledEndsAt;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "status", length = 40, nullable = false)
    @Convert(converter = TournamentMatchStatusConverter.class)
    private TournamentMatchStatus status;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_match_a_id")
    private TournamentMatch parentMatchA;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_match_b_id")
    private TournamentMatch parentMatchB;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    TournamentMatch() {}

    public TournamentMatch(
            final Long id,
            final Tournament tournament,
            final int roundNumber,
            final int matchIndex,
            final TournamentTeam teamA,
            final TournamentTeam teamB,
            final TournamentTeam winnerTeam,
            final Instant scheduledStartsAt,
            final Instant scheduledEndsAt,
            final String address,
            final Double latitude,
            final Double longitude,
            final TournamentMatchStatus status,
            final TournamentMatch parentMatchA,
            final TournamentMatch parentMatchB,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.tournament = tournament;
        this.roundNumber = (short) roundNumber;
        this.matchIndex = (short) matchIndex;
        this.teamA = teamA;
        this.teamB = teamB;
        this.winnerTeam = winnerTeam;
        this.scheduledStartsAt = scheduledStartsAt;
        this.scheduledEndsAt = scheduledEndsAt;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.parentMatchA = parentMatchA;
        this.parentMatchB = parentMatchB;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getMatchIndex() {
        return matchIndex;
    }

    public TournamentTeam getTeamA() {
        return teamA;
    }

    public TournamentTeam getTeamB() {
        return teamB;
    }

    public TournamentTeam getWinnerTeam() {
        return winnerTeam;
    }

    public Instant getScheduledStartsAt() {
        return scheduledStartsAt;
    }

    public Instant getScheduledEndsAt() {
        return scheduledEndsAt;
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

    public TournamentMatchStatus getStatus() {
        return status;
    }

    public TournamentMatch getParentMatchA() {
        return parentMatchA;
    }

    public TournamentMatch getParentMatchB() {
        return parentMatchB;
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

    public void setTournament(final Tournament tournament) {
        this.tournament = tournament;
    }

    public void setTeamA(final TournamentTeam teamA) {
        this.teamA = teamA;
    }

    public void setTeamB(final TournamentTeam teamB) {
        this.teamB = teamB;
    }

    public void setWinnerTeam(final TournamentTeam winnerTeam) {
        this.winnerTeam = winnerTeam;
    }

    public void setScheduledStartsAt(final Instant scheduledStartsAt) {
        this.scheduledStartsAt = scheduledStartsAt;
    }

    public void setScheduledEndsAt(final Instant scheduledEndsAt) {
        this.scheduledEndsAt = scheduledEndsAt;
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

    public void setStatus(final TournamentMatchStatus status) {
        this.status = status;
    }

    public void setParentMatchA(final TournamentMatch parentMatchA) {
        this.parentMatchA = parentMatchA;
    }

    public void setParentMatchB(final TournamentMatch parentMatchB) {
        this.parentMatchB = parentMatchB;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "TournamentMatch{"
                + "id="
                + id
                + ", tournamentId="
                + (tournament == null ? null : tournament.getId())
                + ", roundNumber="
                + roundNumber
                + ", matchIndex="
                + matchIndex
                + ", teamAId="
                + (teamA == null ? null : teamA.getId())
                + ", teamBId="
                + (teamB == null ? null : teamB.getId())
                + ", winnerTeamId="
                + (winnerTeam == null ? null : winnerTeam.getId())
                + ", status="
                + status
                + '}';
    }
}
