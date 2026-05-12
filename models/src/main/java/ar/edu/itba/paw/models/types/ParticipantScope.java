package ar.edu.itba.paw.models.types;

public enum ParticipantScope implements PersistableEnum {
    MATCH("match"),
    SERIES("series");

    private final String dbValue;

    ParticipantScope(String dbValue) {
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
