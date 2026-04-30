package ar.edu.itba.paw.services.exceptions;

public class PlayerReviewException extends RuntimeException {

    public static final String SELF_REVIEW = "self_review";
    public static final String NOT_ELIGIBLE = "not_eligible";
    public static final String INVALID_REACTION = "invalid_reaction";
    public static final String COMMENT_TOO_LONG = "comment_too_long";
    public static final String NOT_FOUND = "not_found";

    private final String code;

    public PlayerReviewException(final String code, final String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
