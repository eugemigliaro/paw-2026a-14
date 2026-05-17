package ar.edu.itba.paw.models.types;

public enum ReportReason implements PersistableEnum {
    INAPPROPRIATE_CONTENT("inappropriate_content"),
    AGGRESSIVE_LANGUAGE("aggressive_language"),
    SPAM("spam"),
    HARASSMENT("harassment"),
    CHEATING("cheating"),
    OTHER("other");

    private final String dbValue;

    ReportReason(final String dbValue) {
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
