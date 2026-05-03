package ar.edu.itba.paw.models;

import java.time.Instant;

public class UserBan {

    private final Long id;
    private final Long moderationReportId;
    private final Instant bannedUntil;

    public UserBan(final Long id, final Long moderationReportId, final Instant bannedUntil) {
        this.id = id;
        this.moderationReportId = moderationReportId;
        this.bannedUntil = bannedUntil;
    }

    public Long getId() {
        return id;
    }

    public Long getModerationReportId() {
        return moderationReportId;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public boolean isActive(final Instant now) {
        return bannedUntil != null && now != null && bannedUntil.isAfter(now);
    }
}
