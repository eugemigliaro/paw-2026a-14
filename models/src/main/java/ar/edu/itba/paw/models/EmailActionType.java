package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum EmailActionType {
    MATCH_RESERVATION("match_reservation");

    private final String dbValue;

    EmailActionType(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<EmailActionType> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(actionType -> actionType.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
