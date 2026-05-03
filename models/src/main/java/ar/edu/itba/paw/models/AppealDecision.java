package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum AppealDecision {
    UPHELD("upheld"),
    LIFTED("lifted");

    private final String dbValue;

    AppealDecision(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Optional<AppealDecision> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(decision -> decision.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
