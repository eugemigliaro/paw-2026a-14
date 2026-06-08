package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationAppealRejectedException extends ModerationException {
    public ModerationAppealRejectedException() {
        super("appealRejected");
    }
}
