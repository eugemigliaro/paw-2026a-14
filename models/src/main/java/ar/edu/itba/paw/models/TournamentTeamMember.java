package ar.edu.itba.paw.models;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.persistence.Column;
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
        name = "tournament_team_members",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"team_id", "user_id"})})
public class TournamentTeamMember {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "tournament_team_members_id_seq")
    @SequenceGenerator(
            sequenceName = "tournament_team_members_id_seq",
            name = "tournament_team_members_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private TournamentTeam team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_captain", nullable = false)
    private boolean captain;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    TournamentTeamMember() {}

    public TournamentTeamMember(
            final Long id,
            final TournamentTeam team,
            final User user,
            final boolean captain,
            final Instant joinedAt) {
        this.id = id;
        this.team = team;
        this.user = user;
        this.captain = captain;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public TournamentTeam getTeam() {
        return team;
    }

    public User getUser() {
        return user;
    }

    public boolean isCaptain() {
        return captain;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public OffsetDateTime getJoinedAtDateTime() {
        return PlatformTime.toOffsetDateTime(joinedAt);
    }

    public Long getVersion() {
        return version;
    }

    public void setTeam(final TournamentTeam team) {
        this.team = team;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public void setCaptain(final boolean captain) {
        this.captain = captain;
    }

    public void setJoinedAt(final Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TournamentTeamMember)) {
            return false;
        }

        TournamentTeamMember that = (TournamentTeamMember) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
