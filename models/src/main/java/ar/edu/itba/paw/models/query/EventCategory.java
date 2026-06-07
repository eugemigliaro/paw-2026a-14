package ar.edu.itba.paw.models.query;

import java.util.Arrays;
import java.util.Optional;

public enum EventCategory {
    HOSTED("hosted"),
    JOINED("joined"),
    INVITED("invited"),
    PENDING("pending");

    private final String queryValue;

    EventCategory(final String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public static Optional<EventCategory> fromQueryValue(final String value) {
        return Arrays.stream(values())
                .filter(category -> category.queryValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
