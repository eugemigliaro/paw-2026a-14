package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum EmailActionStatus {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String dbValue;

    EmailActionStatus(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<EmailActionStatus> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
