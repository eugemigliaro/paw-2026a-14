package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum PlayerReviewReaction {
    LIKE("like"),
    DISLIKE("dislike");

    private final String dbValue;

    PlayerReviewReaction(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<PlayerReviewReaction> fromDbValue(final String dbValue) {
        if (dbValue == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(reaction -> reaction.dbValue.equalsIgnoreCase(dbValue.trim()))
                .findFirst();
    }
}
