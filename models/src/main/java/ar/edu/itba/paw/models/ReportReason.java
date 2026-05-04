package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum ReportReason {
    INAPPROPRIATE_CONTENT("inappropriate_content"),
    AGGRESSIVE_LANGUAGE("aggressive_language"),
    SPAM("spam"),
    HARASSMENT("harassment"),
    CHEATING("cheating"),
    OTHER("other");

    private final String dbValue;

    ReportReason(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<ReportReason> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(reason -> reason.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
