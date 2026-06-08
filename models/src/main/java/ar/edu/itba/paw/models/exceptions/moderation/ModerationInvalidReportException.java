package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationInvalidReportException extends ModerationException {
    public ModerationInvalidReportException() {
        super("invalidReport");
    }
}
