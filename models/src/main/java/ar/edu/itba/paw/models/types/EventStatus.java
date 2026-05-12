package ar.edu.itba.paw.models.types;

public enum EventStatus implements PersistableEnum {
    DRAFT("draft"),
    OPEN("open"),
    CANCELLED("cancelled"),
    COMPLETED("completed");

    private final String dbValue;

    EventStatus(String dbValue) {
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
