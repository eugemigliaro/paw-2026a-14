package ar.edu.itba.paw.models.types;

public enum ReportResolution implements PersistableEnum {
    DISMISSED("dismissed"),
    CONTENT_DELETED("content_deleted"),
    USER_BANNED("user_banned");

    private final String dbValue;

    ReportResolution(final String dbValue) {
        this.dbValue = dbValue;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
