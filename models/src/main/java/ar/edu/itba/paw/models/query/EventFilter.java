package ar.edu.itba.paw.models.query;

import java.util.Arrays;
import java.util.Optional;

public enum EventFilter {
    UPCOMING("upcoming"),
    PAST("past");

    private final String queryValue;

    EventFilter(final String queryValue) {
        this.queryValue = queryValue;
    }

    public String getQueryValue() {
        return queryValue;
    }

    public static Optional<EventFilter> fromQueryValue(final String value) {
        return Arrays.stream(values())
                .filter(filter -> filter.queryValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return queryValue;
    }
}
