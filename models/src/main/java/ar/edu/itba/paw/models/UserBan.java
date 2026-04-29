package ar.edu.itba.paw.models;

import java.time.Instant;

public class UserBan {

    private final Long id;
    private final Long userId;
    private final Long bannedByUserId;
    private final String reason;
    private final Instant bannedUntil;
    private final Instant createdAt;
    private final String appealReason;
    private final int appealCount;
    private final Instant appealedAt;
    private final Instant appealResolvedAt;
    private final Long appealResolvedByUserId;
    private final BanAppealDecision appealDecision;

    public UserBan(
            final Long id,
            final Long userId,
            final Long bannedByUserId,
            final String reason,
            final Instant bannedUntil,
            final Instant createdAt,
            final String appealReason,
            final int appealCount,
            final Instant appealedAt,
            final Instant appealResolvedAt,
            final Long appealResolvedByUserId,
            final BanAppealDecision appealDecision) {
        this.id = id;
        this.userId = userId;
        this.bannedByUserId = bannedByUserId;
        this.reason = reason;
        this.bannedUntil = bannedUntil;
        this.createdAt = createdAt;
        this.appealReason = appealReason;
        this.appealCount = appealCount;
        this.appealedAt = appealedAt;
        this.appealResolvedAt = appealResolvedAt;
        this.appealResolvedByUserId = appealResolvedByUserId;
        this.appealDecision = appealDecision;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBannedByUserId() {
        return bannedByUserId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getBannedUntil() {
        return bannedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getAppealReason() {
        return appealReason;
    }

    public int getAppealCount() {
        return appealCount;
    }

    public Instant getAppealedAt() {
        return appealedAt;
    }

    public Instant getAppealResolvedAt() {
        return appealResolvedAt;
    }

    public Long getAppealResolvedByUserId() {
        return appealResolvedByUserId;
    }

    public BanAppealDecision getAppealDecision() {
        return appealDecision;
    }

    public boolean isActive(final Instant now) {
        return bannedUntil != null && now != null && bannedUntil.isAfter(now);
    }
}
