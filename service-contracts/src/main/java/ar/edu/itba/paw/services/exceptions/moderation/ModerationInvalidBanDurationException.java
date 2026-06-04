package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationInvalidBanDurationException extends ModerationException {
    public ModerationInvalidBanDurationException(final String message) {
        super(message);
    }
}
