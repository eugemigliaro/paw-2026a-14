package ar.edu.itba.paw.services.exceptions.moderation;

public class ModerationValueTooLongException extends ModerationException {
    public ModerationValueTooLongException() {
        super("exception.field.lengthExcedeed");
    }
}
