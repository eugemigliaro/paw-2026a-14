package ar.edu.itba.paw.models.types;

public enum RecurrenceEndMode implements PersistableEnum {
    UNTIL_DATE("until_date"),
    OCCURRENCE_COUNT("occurrence_count");

    private final String dbValue;

    RecurrenceEndMode(final String dbValue) {
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
