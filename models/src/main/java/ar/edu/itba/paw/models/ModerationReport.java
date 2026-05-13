package ar.edu.itba.paw.models;

import ar.edu.itba.paw.models.converters.AppealDecisionConverter;
import ar.edu.itba.paw.models.converters.ReportReasonConverter;
import ar.edu.itba.paw.models.converters.ReportResolutionConverter;
import ar.edu.itba.paw.models.converters.ReportStatusConverter;
import ar.edu.itba.paw.models.converters.ReportTargetTypeConverter;
import ar.edu.itba.paw.models.types.AppealDecision;
import ar.edu.itba.paw.models.types.ReportReason;
import ar.edu.itba.paw.models.types.ReportResolution;
import ar.edu.itba.paw.models.types.ReportStatus;
import ar.edu.itba.paw.models.types.ReportTargetType;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "moderation_reports")
public class ModerationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moderation_reports_id_seq")
    @SequenceGenerator(
            sequenceName = "moderation_reports_id_seq",
            name = "moderation_reports_id_seq",
            allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "target_type", nullable = false)
    @Convert(converter = ReportTargetTypeConverter.class)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "reason", nullable = false)
    @Convert(converter = ReportReasonConverter.class)
    private ReportReason reason;

    @Column(name = "details")
    private String details;

    @Column(name = "status", nullable = false)
    @Convert(converter = ReportStatusConverter.class)
    private ReportStatus status;

    @Column(name = "resolution")
    @Convert(converter = ReportResolutionConverter.class)
    private ReportResolution resolution;

    @Column(name = "resolution_details")
    private String resolutionDetails;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "appeal_reason")
    private String appealReason;

    @Column(name = "appeal_count", nullable = false, columnDefinition = "smallint")
    private short appealCount;

    @Column(name = "appealed_at")
    private Instant appealedAt;

    @Column(name = "appeal_decision")
    @Convert(converter = AppealDecisionConverter.class)
    private AppealDecision appealDecision;

    @Column(name = "appeal_resolved_by_user_id")
    private Long appealResolvedByUserId;

    @Column(name = "appeal_resolved_at")
    private Instant appealResolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Default no-arg constructor for JPA
    ModerationReport() {}

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
            final short appealCount,
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

    public short getAppealCount() {
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

    public void setStatus(final ReportStatus status) {
        this.status = status;
    }

    public void setReviewedByUserId(final Long reviewedByUserId) {
        this.reviewedByUserId = reviewedByUserId;
    }

    public void setReviewedAt(final Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public void setResolution(final ReportResolution resolution) {
        this.resolution = resolution;
    }

    public void setResolutionDetails(final String resolutionDetails) {
        this.resolutionDetails = resolutionDetails;
    }

    public void setAppealReason(final String appealReason) {
        this.appealReason = appealReason;
    }

    public void setAppealCount(final short appealCount) {
        this.appealCount = appealCount;
    }

    public void setAppealedAt(final Instant appealedAt) {
        this.appealedAt = appealedAt;
    }

    public void setAppealDecision(final AppealDecision appealDecision) {
        this.appealDecision = appealDecision;
    }

    public void setAppealResolvedByUserId(final Long appealResolvedByUserId) {
        this.appealResolvedByUserId = appealResolvedByUserId;
    }

    public void setAppealResolvedAt(final Instant appealResolvedAt) {
        this.appealResolvedAt = appealResolvedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
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
