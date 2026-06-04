package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationValueTooLongException extends ModerationException {
    public ModerationValueTooLongException(final String message) {
        super(message);
    }
}
