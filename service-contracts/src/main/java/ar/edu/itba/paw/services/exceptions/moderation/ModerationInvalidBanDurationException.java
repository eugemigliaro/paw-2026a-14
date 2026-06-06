package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationInvalidBanDurationException extends ModerationException {
    public ModerationInvalidBanDurationException() {
        super("exception.moderation.banDuration.invalid");
    }
}
