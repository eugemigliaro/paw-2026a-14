package ar.edu.itba.paw.models.exceptions.moderation;

public class ModerationValueTooLongException extends ModerationException {
    public ModerationValueTooLongException() {
        super("valueTooLong");
    }
}
