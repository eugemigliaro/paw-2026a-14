package ar.edu.itba.paw.models;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "user_bans")
public class UserBan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_bans_id_seq")
    @SequenceGenerator(
            sequenceName = "user_bans_id_seq",
            name = "user_bans_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderation_report_id", nullable = false)
    private ModerationReport moderationReport;

    @Column(name = "banned_until", nullable = false)
    private Instant bannedUntil;

    // Default no-arg constructor for JPA
    UserBan() {}

    public UserBan(
            final Long id, final ModerationReport moderationReport, final Instant bannedUntil) {
        this.id = id;
        this.moderationReport = moderationReport;
        this.bannedUntil = bannedUntil;
    }

    public Long getId() {
        return id;
    }

    public ModerationReport getModerationReport() {
        return moderationReport;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(Instant bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public boolean isActive(final Instant now) {
        return bannedUntil != null && now != null && bannedUntil.isAfter(now);
    }

    @Override
    public String toString() {
        return "UserBan{"
                + "id="
                + id
                + ", moderationReport="
                + (moderationReport != null ? moderationReport.getId() : null)
                + ", bannedUntil="
                + bannedUntil
                + '}';
    }
}
