package ar.edu.itba.paw.models.types;

public enum EventType implements PersistableEnum {
    MATCH("match"),
    TOURNAMENT("tournament");

    private final String dbValue;

    EventType(String dbValue) {
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
