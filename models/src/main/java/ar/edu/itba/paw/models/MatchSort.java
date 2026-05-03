package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum MatchSort {
    SOONEST("soonest"),
    PRICE_LOW("price"),
    SPOTS_DESC("spots");

    private final String queryValue;

    MatchSort(final String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public static Optional<MatchSort> fromQueryValue(final String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.queryValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
