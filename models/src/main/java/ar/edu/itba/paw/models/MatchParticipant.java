package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.ParticipantScopeConverter;
import ar.edu.itba.paw.models.converters.ParticipantStatusConverter;
import ar.edu.itba.paw.models.types.ParticipantScope;
import ar.edu.itba.paw.models.types.ParticipantStatus;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "match_participants",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"match_id", "user_id"})})
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_participants_id_seq")
    @SequenceGenerator(
            sequenceName = "match_participants_id_seq",
            name = "match_participants_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "status", length = 30, nullable = false)
    @Convert(converter = ParticipantStatusConverter.class)
    private ParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "scope", length = 20, nullable = false)
    @Convert(converter = ParticipantScopeConverter.class)
    private ParticipantScope scope;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    MatchParticipant() {}

    public MatchParticipant(
            Match match,
            User user,
            ParticipantStatus status,
            Instant joinedAt,
            ParticipantScope scope) {
        this.match = match;
        this.user = user;
        this.status = status;
        this.joinedAt = joinedAt;
        this.scope = scope;
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public User getUser() {
        return user;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(ParticipantStatus status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isSeriesScope() {
        return scope == ParticipantScope.SERIES;
    }

    public ParticipantScope getScope() {
        return scope;
    }

    public void setScope(ParticipantScope scope) {
        this.scope = scope;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
