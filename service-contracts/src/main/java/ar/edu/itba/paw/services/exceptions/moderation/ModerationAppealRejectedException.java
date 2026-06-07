package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationAppealRejectedException extends ModerationException {
    public ModerationAppealRejectedException() {
        super("appealRejected");
    }
}
