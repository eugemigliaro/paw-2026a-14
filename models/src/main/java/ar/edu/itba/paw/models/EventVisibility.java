package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum EventVisibility {
    PUBLIC("public"),
    PRIVATE("private"),
    INVITE_ONLY("invite_only");

    private final String value;

    EventVisibility(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<EventVisibility> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(visibility -> visibility.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
