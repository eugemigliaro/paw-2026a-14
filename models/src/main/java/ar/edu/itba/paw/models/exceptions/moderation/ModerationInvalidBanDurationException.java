package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationInvalidBanDurationException extends ModerationException {
    public ModerationInvalidBanDurationException() {
        super("invalidBanDuration");
    }
}
