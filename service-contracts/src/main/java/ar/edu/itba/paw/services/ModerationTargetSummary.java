package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.ReportTargetType;

public class ModerationTargetSummary {

    private final ReportTargetType targetType;
    private final Long targetId;
    private final String displayName;
    private final String targetSlug;
    private final boolean found;

    public ModerationTargetSummary(
            final ReportTargetType targetType,
            final Long targetId,
            final String displayName,
            final boolean found) {
        this(targetType, targetId, displayName, null, found);
    }

    public ModerationTargetSummary(
            final ReportTargetType targetType,
            final Long targetId,
            final String displayName,
            final String targetSlug,
            final boolean found) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.displayName = displayName;
        this.targetSlug = targetSlug;
        this.found = found;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTargetSlug() {
        return targetSlug;
    }

    public boolean isFound() {
        return found;
    }
}
