package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationSelfReportException extends ModerationException {
    public ModerationSelfReportException() {
        super("selfReport");
    }
}
