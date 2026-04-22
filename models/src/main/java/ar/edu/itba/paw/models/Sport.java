package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum Sport {
    FOOTBALL("football", "Football"),
    TENNIS("tennis", "Tennis"),
    PADEL("padel", "Padel"),
    BASKETBALL("basketball", "Basketball"),
    OTHER("other", "Other");

    private final String dbValue;
    private final String displayName;

    Sport(final String dbValue, final String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<Sport> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(sport -> sport.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
