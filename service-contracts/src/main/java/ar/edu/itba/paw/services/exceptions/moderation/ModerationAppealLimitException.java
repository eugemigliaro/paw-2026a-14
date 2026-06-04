package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationAppealLimitException extends ModerationException {
    public ModerationAppealLimitException(final String message) {
        super(message);
    }
}
