package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationSelfReportException extends ModerationException {
    public ModerationSelfReportException() {
        super("moderation.report.error.self");
    }
}
