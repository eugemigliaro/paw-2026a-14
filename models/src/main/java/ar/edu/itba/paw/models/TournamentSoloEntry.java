package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.TournamentSoloEntryStatusConverter;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "tournament_solo_entries",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tournament_id", "user_id"})})
public class TournamentSoloEntry {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "tournament_solo_entries_id_seq")
    @SequenceGenerator(
            sequenceName = "tournament_solo_entries_id_seq",
            name = "tournament_solo_entries_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", length = 40, nullable = false)
    @Convert(converter = TournamentSoloEntryStatusConverter.class)
    private TournamentSoloEntryStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private TournamentTeam assignedTeam;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    TournamentSoloEntry() {}

    public TournamentSoloEntry(
            final Long id,
            final Tournament tournament,
            final User user,
            final TournamentSoloEntryStatus status,
            final TournamentTeam assignedTeam,
            final Instant joinedAt,
            final Instant leftAt) {
        this.id = id;
        this.tournament = tournament;
        this.user = user;
        this.status = status;
        this.assignedTeam = assignedTeam;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public User getUser() {
        return user;
    }

    public TournamentSoloEntryStatus getStatus() {
        return status;
    }

    public TournamentTeam getAssignedTeam() {
        return assignedTeam;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public OffsetDateTime getJoinedAtDateTime() {
        return PlatformTime.toOffsetDateTime(joinedAt);
    }

    public Instant getLeftAt() {
        return leftAt;
    }

    public OffsetDateTime getLeftAtDateTime() {
        return PlatformTime.toOffsetDateTime(leftAt);
    }

    public Long getVersion() {
        return version;
    }

    public void setTournament(final Tournament tournament) {
        this.tournament = tournament;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public void setStatus(final TournamentSoloEntryStatus status) {
        this.status = status;
    }

    public void setAssignedTeam(final TournamentTeam assignedTeam) {
        this.assignedTeam = assignedTeam;
    }

    public void setJoinedAt(final Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void setLeftAt(final Instant leftAt) {
        this.leftAt = leftAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TournamentSoloEntry)) {
            return false;
        }

        TournamentSoloEntry that = (TournamentSoloEntry) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
