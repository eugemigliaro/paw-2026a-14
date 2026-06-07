package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationInvalidBanDurationException extends ModerationException {
    public ModerationInvalidBanDurationException() {
        super("invalidBanDuration");
    }
}
