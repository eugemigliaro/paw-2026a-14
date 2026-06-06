package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.SportConverter;
import ar.edu.itba.paw.models.types.Sport;
import java.time.Instant;
import java.time.OffsetDateTime;
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
        name = "user_sport_ratings",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "sport"})})
public class UserSportRating {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sport_ratings_id_seq")
    @SequenceGenerator(
            sequenceName = "user_sport_ratings_id_seq",
            name = "user_sport_ratings_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sport", length = 30, nullable = false)
    @Convert(converter = SportConverter.class)
    private Sport sport;

    @Column(name = "elo", nullable = false)
    private int elo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    UserSportRating() {}

    public UserSportRating(
            final Long id,
            final User user,
            final Sport sport,
            final int elo,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.user = user;
        this.sport = sport;
        this.elo = elo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Sport getSport() {
        return sport;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(final int elo) {
        this.elo = elo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCreatedAtDateTime() {
        return PlatformTime.toOffsetDateTime(createdAt);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getUpdatedAtDateTime() {
        return PlatformTime.toOffsetDateTime(updatedAt);
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
