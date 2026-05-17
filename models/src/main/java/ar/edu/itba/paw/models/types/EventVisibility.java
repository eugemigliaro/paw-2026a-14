package ar.edu.itba.paw.models.types;

public enum EventVisibility implements PersistableEnum {
    PUBLIC("public"),
    PRIVATE("private"),
    INVITE_ONLY("invite_only");

    private final String dbValue;

    EventVisibility(String dbValue) {
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
