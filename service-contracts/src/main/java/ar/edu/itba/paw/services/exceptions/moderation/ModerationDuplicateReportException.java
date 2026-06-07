package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationDuplicateReportException extends ModerationException {
    public ModerationDuplicateReportException() {
        super("duplicateReport");
    }
}
