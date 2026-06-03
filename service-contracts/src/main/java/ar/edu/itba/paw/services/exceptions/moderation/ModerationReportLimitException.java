package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationReportLimitException extends ModerationException {
    public ModerationReportLimitException(final String message) {
        super(message);
    }
}
