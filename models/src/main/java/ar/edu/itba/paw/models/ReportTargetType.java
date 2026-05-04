package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum ReportTargetType {
    MATCH("match"),
    REVIEW("review"),
    USER("user");

    private final String dbValue;

    ReportTargetType(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<ReportTargetType> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(type -> type.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
