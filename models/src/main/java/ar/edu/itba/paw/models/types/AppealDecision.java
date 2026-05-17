package ar.edu.itba.paw.models.types;

public enum AppealDecision implements PersistableEnum {
    UPHELD("upheld"),
    LIFTED("lifted");

    private final String dbValue;

    AppealDecision(final String dbValue) {
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
