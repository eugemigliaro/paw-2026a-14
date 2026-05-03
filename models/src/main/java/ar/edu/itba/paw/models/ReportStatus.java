package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum ReportStatus {
    PENDING("pending"),
    UNDER_REVIEW("under_review"),
    RESOLVED("resolved"),
    APPEALED("appealed"),
    FINALIZED("finalized");

    private final String dbValue;

    ReportStatus(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<ReportStatus> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(status -> status.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
