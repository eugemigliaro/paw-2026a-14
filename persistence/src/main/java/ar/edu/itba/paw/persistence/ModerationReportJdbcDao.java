package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.AppealDecision;
import ar.edu.itba.paw.models.ModerationReport;
import ar.edu.itba.paw.models.ReportReason;
import ar.edu.itba.paw.models.ReportResolution;
import ar.edu.itba.paw.models.ReportStatus;
import ar.edu.itba.paw.models.ReportTargetType;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ModerationReportJdbcDao implements ModerationReportDao {

    @NonNull
    private static final RowMapper<ModerationReport> MODERATION_REPORT_ROW_MAPPER =
            (ResultSet rs, int rowNum) ->
                    new ModerationReport(
                            rs.getLong("id"),
                            rs.getLong("reporter_user_id"),
                            ReportTargetType.fromDbValue(rs.getString("target_type")).orElse(null),
                            rs.getLong("target_id"),
                            ReportReason.fromDbValue(rs.getString("reason")).orElse(null),
                            rs.getString("details"),
                            ReportStatus.fromDbValue(rs.getString("status"))
                                    .orElse(ReportStatus.PENDING),
                            ReportResolution.fromDbValue(rs.getString("resolution")).orElse(null),
                            rs.getString("resolution_details"),
                            rs.getObject("reviewed_by_user_id") == null
                                    ? null
                                    : rs.getLong("reviewed_by_user_id"),
                            toInstant(rs.getTimestamp("reviewed_at")),
                            rs.getString("appeal_reason"),
                            rs.getInt("appeal_count"),
                            toInstant(rs.getTimestamp("appealed_at")),
                            AppealDecision.fromDbValue(rs.getString("appeal_decision"))
                                    .orElse(null),
                            rs.getObject("appeal_resolved_by_user_id") == null
                                    ? null
                                    : rs.getLong("appeal_resolved_by_user_id"),
                            toInstant(rs.getTimestamp("appeal_resolved_at")),
                            toInstant(rs.getTimestamp("created_at")),
                            toInstant(rs.getTimestamp("updated_at")));

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;

    @Autowired
    public ModerationReportJdbcDao(final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert =
                new SimpleJdbcInsert(dataSource)
                        .withTableName("moderation_reports")
                        .usingGeneratedKeyColumns("id")
                        .usingColumns(
                                "reporter_user_id",
                                "target_type",
                                "target_id",
                                "reason",
                                "details",
                                "status",
                                "created_at",
                                "updated_at",
                                "appeal_count");
    }

    @Override
    public ModerationReport createReport(
            final Long reporterUserId,
            final ReportTargetType targetType,
            final Long targetId,
            final ReportReason reason,
            final String details) {
        final Instant now = Instant.now();
        final Map<String, Object> values = new HashMap<>();
        values.put("reporter_user_id", reporterUserId);
        values.put("target_type", new SqlParameterValue(Types.OTHER, targetType.getDbValue()));
        values.put("target_id", targetId);
        values.put("reason", new SqlParameterValue(Types.OTHER, reason.getDbValue()));
        values.put("details", details);
        values.put("status", new SqlParameterValue(Types.OTHER, ReportStatus.PENDING.getDbValue()));
        values.put("created_at", Timestamp.from(now));
        values.put("updated_at", Timestamp.from(now));
        values.put("appeal_count", 0);

        final Long id = jdbcInsert.executeAndReturnKey(values).longValue();

        return new ModerationReport(
                id,
                reporterUserId,
                targetType,
                targetId,
                reason,
                details,
                ReportStatus.PENDING,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                null,
                now,
                now);
    }

    @Override
    public Optional<ModerationReport> findById(final Long reportId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM moderation_reports WHERE id = ?",
                        MODERATION_REPORT_ROW_MAPPER,
                        reportId)
                .stream()
                .findFirst();
    }

    @Override
    public List<ModerationReport> findReportsByReporter(final Long reporterUserId) {
        return findReportsByReporter(reporterUserId, List.of(), List.of());
    }

    @Override
    public List<ModerationReport> findReportsByReporter(
            final Long reporterUserId,
            final List<ReportTargetType> targetTypes,
            final List<ReportStatus> statuses) {
        final StringBuilder sql =
                new StringBuilder("SELECT * FROM moderation_reports WHERE reporter_user_id = ?");
        final List<Object> args = new LinkedList<>();
        args.add(reporterUserId);

        appendEnumFilter(sql, args, "target_type", targetTypes);
        appendEnumFilter(sql, args, "status", statuses);

        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), MODERATION_REPORT_ROW_MAPPER, args.toArray());
    }

    @Override
    public List<ModerationReport> findReports() {
        return jdbcTemplate.query(
                "SELECT * FROM moderation_reports ORDER BY updated_at DESC, id DESC",
                MODERATION_REPORT_ROW_MAPPER);
    }

    @Override
    public List<ModerationReport> findReports(
            List<ReportTargetType> targetTypes, List<ReportStatus> statuses) {
        final StringBuilder sql = new StringBuilder("SELECT * FROM moderation_reports");
        final List<Object> args = new LinkedList<>();

        sql.append(" WHERE 1=1");

        appendEnumFilter(sql, args, "target_type", targetTypes);
        appendEnumFilter(sql, args, "status", statuses);

        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbcTemplate.query(sql.toString(), MODERATION_REPORT_ROW_MAPPER, args.toArray());
    }

    @Override
    public Optional<ModerationReport> findLatestUserBanReportByTargetUserId(
            final Long targetUserId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM moderation_reports"
                                + " WHERE target_type = ? AND target_id = ?"
                                + " AND resolution = ?"
                                + " ORDER BY updated_at DESC, id DESC LIMIT 1",
                        MODERATION_REPORT_ROW_MAPPER,
                        new SqlParameterValue(Types.OTHER, ReportTargetType.USER.getDbValue()),
                        targetUserId,
                        new SqlParameterValue(
                                Types.OTHER, ReportResolution.USER_BANNED.getDbValue()))
                .stream()
                .findFirst();
    }

    @Override
    public int countActiveReportsByReporter(final Long reporterUserId) {
        final Integer count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM moderation_reports WHERE reporter_user_id = ? AND status IN (?,?,?)",
                        Integer.class,
                        reporterUserId,
                        new SqlParameterValue(Types.OTHER, ReportStatus.PENDING.getDbValue()),
                        new SqlParameterValue(Types.OTHER, ReportStatus.UNDER_REVIEW.getDbValue()),
                        new SqlParameterValue(Types.OTHER, ReportStatus.APPEALED.getDbValue()));
        return count == null ? 0 : count;
    }

    @Override
    public boolean markUnderReview(
            final Long reportId, final Long reviewedByUserId, final Instant reviewedAt) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE moderation_reports"
                                + " SET status = ?, reviewed_by_user_id = ?,"
                                + " reviewed_at = ?, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND status = ?",
                        new SqlParameterValue(Types.OTHER, ReportStatus.UNDER_REVIEW.getDbValue()),
                        reviewedByUserId,
                        Timestamp.from(reviewedAt),
                        reportId,
                        new SqlParameterValue(Types.OTHER, ReportStatus.PENDING.getDbValue()));
        return rows == 1;
    }

    @Override
    public boolean resolveReport(
            final Long reportId,
            final Long reviewedByUserId,
            final ReportResolution resolution,
            final String resolutionDetails,
            final Instant reviewedAt,
            final ReportStatus nextStatus) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE moderation_reports"
                                + " SET status = ?, resolution = ?, resolution_details = ?,"
                                + " reviewed_by_user_id = ?, reviewed_at = ?,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND status IN (?,?)",
                        new SqlParameterValue(Types.OTHER, nextStatus.getDbValue()),
                        new SqlParameterValue(Types.OTHER, resolution.getDbValue()),
                        resolutionDetails,
                        reviewedByUserId,
                        Timestamp.from(reviewedAt),
                        reportId,
                        new SqlParameterValue(Types.OTHER, ReportStatus.PENDING.getDbValue()),
                        new SqlParameterValue(Types.OTHER, ReportStatus.UNDER_REVIEW.getDbValue()));
        return rows == 1;
    }

    @Override
    public boolean appealReport(
            final Long reportId, final String appealReason, final Instant appealedAt) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE moderation_reports"
                                + " SET status = ?, appeal_reason = ?, appeal_count = 1,"
                                + " appealed_at = ?, updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND status = ? AND appeal_count = 0",
                        new SqlParameterValue(Types.OTHER, ReportStatus.APPEALED.getDbValue()),
                        appealReason,
                        Timestamp.from(appealedAt),
                        reportId,
                        new SqlParameterValue(Types.OTHER, ReportStatus.RESOLVED.getDbValue()));
        return rows == 1;
    }

    @Override
    public boolean finalizeAppeal(
            final Long reportId,
            final Long appealResolvedByUserId,
            final AppealDecision appealDecision,
            final Instant appealResolvedAt) {
        final int rows =
                jdbcTemplate.update(
                        "UPDATE moderation_reports"
                                + " SET status = ?, appeal_decision = ?,"
                                + " appeal_resolved_by_user_id = ?, appeal_resolved_at = ?,"
                                + " updated_at = CURRENT_TIMESTAMP"
                                + " WHERE id = ? AND status = ?",
                        new SqlParameterValue(Types.OTHER, ReportStatus.FINALIZED.getDbValue()),
                        new SqlParameterValue(Types.OTHER, appealDecision.getDbValue()),
                        appealResolvedByUserId,
                        Timestamp.from(appealResolvedAt),
                        reportId,
                        new SqlParameterValue(Types.OTHER, ReportStatus.APPEALED.getDbValue()));
        return rows == 1;
    }

    private static <T> void appendEnumFilter(
            final StringBuilder sql,
            final List<Object> args,
            final String column,
            final List<T> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        sql.append(" AND ").append(column).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append('?');
            if (values.get(i) instanceof ReportTargetType targetType) {
                args.add(new SqlParameterValue(Types.OTHER, targetType.getDbValue()));
            } else if (values.get(i) instanceof ReportStatus status) {
                args.add(new SqlParameterValue(Types.OTHER, status.getDbValue()));
            }
        }
        sql.append(')');
    }

    private static Instant toInstant(final Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
