package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum EventJoinPolicy {
    DIRECT("direct"),
    APPROVAL_REQUIRED("approval_required"),
    INVITE_ONLY("invite_only");

    private final String value;

    EventJoinPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<EventJoinPolicy> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(joinPolicy -> joinPolicy.value.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return value;
    }
}
