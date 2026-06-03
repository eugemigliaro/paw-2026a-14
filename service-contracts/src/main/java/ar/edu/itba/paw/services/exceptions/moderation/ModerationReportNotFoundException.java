package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationReportNotFoundException extends ModerationException {
    public ModerationReportNotFoundException(final String message) {
        super(message);
    }
}
