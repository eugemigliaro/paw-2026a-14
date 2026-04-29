package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum ReviewDeleteReason {
    INAPPROPRIATE_CONTENT("inappropriate_content"),
    AGGRESSIVE_LANGUAGE("aggressive_language"),
    SPAM("spam"),
    OTHER("other");

    private final String dbValue;

    ReviewDeleteReason(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<ReviewDeleteReason> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(reason -> reason.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
