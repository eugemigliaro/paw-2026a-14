package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationReportLimitException extends ModerationException {
    public ModerationReportLimitException() {
        super("reportLimit");
    }
}
