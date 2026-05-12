package ar.edu.itba.paw.models.types;

public enum EmailActionStatus implements PersistableEnum {
    PENDING("pending"),
    COMPLETED("completed"),
    FAILED("failed"),
    EXPIRED("expired");

    private final String dbValue;

    EmailActionStatus(final String dbValue) {
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
