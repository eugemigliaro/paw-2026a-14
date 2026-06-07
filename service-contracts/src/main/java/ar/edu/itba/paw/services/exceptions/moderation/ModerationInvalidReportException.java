package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationInvalidReportException extends ModerationException {
    public ModerationInvalidReportException() {
        super("invalidReport");
    }
}
