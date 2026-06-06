package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationReportFailedException extends ModerationException {
    public ModerationReportFailedException() {
        super("exception.moderation.report.failed");
    }
}
