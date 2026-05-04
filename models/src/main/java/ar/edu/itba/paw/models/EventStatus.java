package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum EventStatus {
    DRAFT("draft"),
    OPEN("open"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    private final String value;

    EventStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<EventStatus> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
