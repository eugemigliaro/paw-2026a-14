package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationReportFailedException extends ModerationException {
    public ModerationReportFailedException() {
        super("reportFailed");
    }
}
