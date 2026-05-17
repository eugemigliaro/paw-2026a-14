package ar.edu.itba.paw.models.types;

public enum UserRole implements PersistableEnum {
    USER("user"),
    ADMIN_MOD("admin_mod");

    private final String dbValue;

    UserRole(final String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    public boolean isAdmin() {
        return this == ADMIN_MOD;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
