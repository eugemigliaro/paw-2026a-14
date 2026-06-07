package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.types.ReportTargetType;

public class ModerationTargetSummary {

    private final ReportTargetType targetType;
    private final Long targetId;
    private final String displayName;
    private final boolean found;

    public ModerationTargetSummary(
            final ReportTargetType targetType,
            final Long targetId,
            final String displayName,
            final boolean found) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.displayName = displayName;
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

    public boolean isFound() {
        return found;
    }
}
