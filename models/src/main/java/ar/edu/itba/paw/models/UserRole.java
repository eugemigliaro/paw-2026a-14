package ar.edu.itba.paw.models;

import java.util.Arrays;
import java.util.Optional;

public enum UserRole {
    USER("user"),
    ADMIN_MOD("admin_mod");

    private final String dbValue;

    UserRole(final String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isAdmin() {
        return this == ADMIN_MOD;
    }

    public static Optional<UserRole> fromDbValue(final String value) {
        return Arrays.stream(values())
                .filter(role -> role.dbValue.equalsIgnoreCase(value))
                .findFirst();
    }
}
