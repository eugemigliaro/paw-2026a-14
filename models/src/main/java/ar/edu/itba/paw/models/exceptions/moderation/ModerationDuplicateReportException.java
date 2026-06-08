package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationDuplicateReportException extends ModerationException {
    public ModerationDuplicateReportException() {
        super("duplicateReport");
    }
}
