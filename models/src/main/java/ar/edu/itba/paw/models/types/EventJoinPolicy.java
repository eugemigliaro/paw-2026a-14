package ar.edu.itba.paw.models.types;

public enum EventJoinPolicy implements PersistableEnum {
    DIRECT("direct"),
    APPROVAL_REQUIRED("approval_required"),
    INVITE_ONLY("invite_only");

    private final String dbValue;

    EventJoinPolicy(String dbValue) {
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
