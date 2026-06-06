package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationTargetNotFoundException extends ModerationException {
    public ModerationTargetNotFoundException() {
        super("exception.moderation.target.notFound");
    }
}
