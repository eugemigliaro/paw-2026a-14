package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum RecurrenceFrequency {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    RecurrenceFrequency(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<RecurrenceFrequency> fromValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }

        final String normalized = value.trim();
        return Arrays.stream(values())
                .filter(frequency -> frequency.value.equalsIgnoreCase(normalized))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
