package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum RecurrenceEndMode {
    UNTIL_DATE("until_date"),
    OCCURRENCE_COUNT("occurrence_count");

    private final String value;

    RecurrenceEndMode(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<RecurrenceEndMode> fromValue(final String value) {
        if (value == null) {
            return Optional.empty();
        }

        final String normalized = value.trim();
        return Arrays.stream(values())
                .filter(mode -> mode.value.equalsIgnoreCase(normalized))
                .findFirst();
    }
}
