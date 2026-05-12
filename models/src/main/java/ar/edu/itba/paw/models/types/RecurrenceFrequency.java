package ar.edu.itba.paw.models.types;

public enum RecurrenceFrequency implements PersistableEnum {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String dbValue;

    RecurrenceFrequency(final String dbValue) {
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
