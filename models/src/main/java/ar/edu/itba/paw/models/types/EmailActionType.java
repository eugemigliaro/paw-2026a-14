package ar.edu.itba.paw.models.types;

public enum EmailActionType implements PersistableEnum {
    MATCH_RESERVATION("match_reservation"),
    MATCH_CREATION("match_creation"),
    ACCOUNT_VERIFICATION("account_verification"),
    PASSWORD_RESET("password_reset");

    private final String dbValue;

    EmailActionType(final String dbValue) {
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
