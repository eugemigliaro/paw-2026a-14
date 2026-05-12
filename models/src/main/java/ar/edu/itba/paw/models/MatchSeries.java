package ar.edu.itba.paw.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "match_series")
public class MatchSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "match_series_id_seq")
    @SequenceGenerator(
            sequenceName = "match_series_id_seq",
            name = "match_series_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_id", nullable = false)
    private UserAccount host;

    @Transient private Long hostUserId;

    @Column(name = "frequency", length = 30, nullable = false)
    private String frequency;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "timezone", length = 100, nullable = false)
    private String timezone;

    @Column(name = "until_date")
    private LocalDate untilDate;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "series")
    private List<Match> occurrences;

    MatchSeries() {}

    public MatchSeries(
            final Long hostUserId,
            final String frequency,
            final Instant startsAt,
            final Instant endsAt,
            final String timezone,
            final LocalDate untilDate,
            final Integer occurrenceCount,
            final Instant createdAt,
            final Instant updatedAt) {
        this.hostUserId = hostUserId;
        this.frequency = frequency;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.timezone = timezone;
        this.untilDate = untilDate;
        this.occurrenceCount = occurrenceCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setHost(final UserAccount host) {
        this.host = host;
        this.hostUserId = host == null ? null : host.getId();
    }
}
