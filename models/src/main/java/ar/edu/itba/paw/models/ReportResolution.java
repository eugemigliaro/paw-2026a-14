package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum ReportResolution {
    DISMISSED("dismissed"),
    WARNING("warning"),
    CONTENT_DELETED("content_deleted"),
    USER_BANNED("user_banned");

    private final String dbValue;

    ReportResolution(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<ReportResolution> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(resolution -> resolution.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
