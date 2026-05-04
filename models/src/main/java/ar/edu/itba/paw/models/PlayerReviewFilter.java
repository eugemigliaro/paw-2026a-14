package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum PlayerReviewFilter {
    BOTH("both", null),
    POSITIVE("positive", PlayerReviewReaction.LIKE),
    BAD("bad", PlayerReviewReaction.DISLIKE);

    private final String queryValue;
    private final PlayerReviewReaction reaction;

    PlayerReviewFilter(final String queryValue, final PlayerReviewReaction reaction) {
        this.queryValue = queryValue;
        this.reaction = reaction;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public Optional<PlayerReviewReaction> getReaction() {
        return Optional.ofNullable(reaction);
    }

    public static PlayerReviewFilter fromQueryValueOrDefault(final String queryValue) {
        return fromQueryValue(queryValue).orElse(BOTH);
    }

    public static Optional<PlayerReviewFilter> fromQueryValue(final String queryValue) {
        if (queryValue == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(filter -> filter.queryValue.equalsIgnoreCase(queryValue.trim()))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
