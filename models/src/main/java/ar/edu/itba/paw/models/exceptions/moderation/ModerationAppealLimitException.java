package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationAppealLimitException extends ModerationException {
    public ModerationAppealLimitException() {
        super("appealLimit");
    }
}
