package ar.edu.itba.paw.models.types;

public enum ReportTargetType implements PersistableEnum {
    MATCH("match"),
    REVIEW("review"),
    USER("user");

    private final String dbValue;

    ReportTargetType(final String dbValue) {
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
