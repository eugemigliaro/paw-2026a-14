package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationTargetNotFoundException extends ModerationException {
    public ModerationTargetNotFoundException() {
        super("targetNotFound");
    }
}
