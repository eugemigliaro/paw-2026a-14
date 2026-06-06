package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationReportLimitException extends ModerationException {
    public ModerationReportLimitException() {
        super("moderation.report.error.limit");
    }
}
