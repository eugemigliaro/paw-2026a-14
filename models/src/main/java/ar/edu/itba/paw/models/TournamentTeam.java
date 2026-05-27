package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.TournamentTeamOriginConverter;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

@Entity
@Table(
        name = "tournament_teams",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"tournament_id", "name"})})
public class TournamentTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tournament_teams_id_seq")
    @SequenceGenerator(
            sequenceName = "tournament_teams_id_seq",
            name = "tournament_teams_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "origin", length = 40, nullable = false)
    @Convert(converter = TournamentTeamOriginConverter.class)
    private TournamentTeamOrigin origin;

    @Column(name = "seed_position")
    private Short seedPosition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
    private List<TournamentTeamMember> members;

    TournamentTeam() {}

    public TournamentTeam(
            final Long id,
            final Tournament tournament,
            final String name,
            final TournamentTeamOrigin origin,
            final Integer seedPosition,
            final Instant createdAt) {
        this.id = id;
        this.tournament = tournament;
        this.name = name;
        this.origin = origin;
        this.seedPosition = seedPosition == null ? null : seedPosition.shortValue();
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public String getName() {
        return name;
    }

    public TournamentTeamOrigin getOrigin() {
        return origin;
    }

    public Integer getSeedPosition() {
        return seedPosition == null ? null : seedPosition.intValue();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<TournamentTeamMember> getMembers() {
        return members;
    }

    public void setTournament(final Tournament tournament) {
        this.tournament = tournament;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setOrigin(final TournamentTeamOrigin origin) {
        this.origin = origin;
    }

    public void setSeedPosition(final Integer seedPosition) {
        this.seedPosition = seedPosition == null ? null : seedPosition.shortValue();
    }

    @Override
    public String toString() {
        return "TournamentTeam{"
                + "id="
                + id
                + ", tournamentId="
                + (tournament == null ? null : tournament.getId())
                + ", name='"
                + name
                + '\''
                + ", origin="
                + origin
                + ", seedPosition="
                + seedPosition
                + '}';
    }
}
