package ar.edu.itba.paw.models.query;

import java.util.Arrays;
import java.util.Optional;

public enum TournamentSort {
    SOONEST("soonest"),
    PRICE("price"),
    DISTANCE("distance");

    private final String queryValue;

    TournamentSort(final String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public static Optional<TournamentSort> fromQueryValue(final String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.queryValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
