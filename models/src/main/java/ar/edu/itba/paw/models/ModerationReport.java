package ar.edu.itba.paw.models;

import java.time.Instant;

public class ModerationReport {

    private final Long id;
    private final Long reporterUserId;
    private final ReportTargetType targetType;
    private final Long targetId;
    private final ReportReason reason;
    private final String details;
    private final ReportStatus status;
    private final ReportResolution resolution;
    private final String resolutionDetails;
    private final Long reviewedByUserId;
    private final Instant reviewedAt;
    private final String appealReason;
    private final int appealCount;
    private final Instant appealedAt;
    private final AppealDecision appealDecision;
    private final Long appealResolvedByUserId;
    private final Instant appealResolvedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public ModerationReport(
            final Long id,
            final Long reporterUserId,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason,
            final String details,
            final ReportStatus status,
            final ReportResolution resolution,
            final String resolutionDetails,
            final Long reviewedByUserId,
            final Instant reviewedAt,
            final String appealReason,
            final int appealCount,
            final Instant appealedAt,
            final AppealDecision appealDecision,
            final Long appealResolvedByUserId,
            final Instant appealResolvedAt,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.reporterUserId = reporterUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.details = details;
        this.status = status;
        this.resolution = resolution;
        this.resolutionDetails = resolutionDetails;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedAt = reviewedAt;
        this.appealReason = appealReason;
        this.appealCount = appealCount;
        this.appealedAt = appealedAt;
        this.appealDecision = appealDecision;
        this.appealResolvedByUserId = appealResolvedByUserId;
        this.appealResolvedAt = appealResolvedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReporterUserId() {
        return reporterUserId;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportResolution getResolution() {
        return resolution;
    }

    public String getResolutionDetails() {
        return resolutionDetails;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
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

    public AppealDecision getAppealDecision() {
        return appealDecision;
    }

    public Long getAppealResolvedByUserId() {
        return appealResolvedByUserId;
    }

    public Instant getAppealResolvedAt() {
        return appealResolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "ModerationReport{"
                + "id="
                + id
                + ", reporterUserId="
                + reporterUserId
                + ", targetType="
                + targetType
                + ", targetId="
                + targetId
                + ", reason="
                + reason
                + ", status="
                + status
                + ", resolution="
                + resolution
                + ", reviewedByUserId="
                + reviewedByUserId
                + ", reviewedAt="
                + reviewedAt
                + ", appealCount="
                + appealCount
                + ", appealedAt="
                + appealedAt
                + ", appealDecision="
                + appealDecision
                + ", appealResolvedByUserId="
                + appealResolvedByUserId
                + ", appealResolvedAt="
                + appealResolvedAt
                + ", createdAt="
                + createdAt
                + ", updatedAt="
                + updatedAt
                + ", hasDetails="
                + (details != null && !details.isBlank())
                + ", hasResolutionDetails="
                + (resolutionDetails != null && !resolutionDetails.isBlank())
                + ", hasAppealReason="
                + (appealReason != null && !appealReason.isBlank())
                + '}';
    }
}
