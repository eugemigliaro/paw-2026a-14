package ar.edu.itba.paw.models.query;

import java.util.Arrays;
import java.util.Optional;

public enum EventSort {
    SOONEST("soonest"),
    PRICE_LOW("price"),
    SPOTS_DESC("spots"),
    DISTANCE("distance");

    private final String queryValue;

    EventSort(final String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public static Optional<EventSort> fromQueryValue(final String value) {
        return Arrays.stream(values())
                .filter(sort -> sort.queryValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
