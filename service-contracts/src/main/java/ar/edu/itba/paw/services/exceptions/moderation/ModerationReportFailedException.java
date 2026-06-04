package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationReportFailedException extends ModerationException {
    public ModerationReportFailedException(final String message) {
        super(message);
    }
}
