package ar.edu.itba.paw.models.types;

public enum TournamentMatchStatus implements PersistableEnum {
    PENDING("pending"),
    SCHEDULED("scheduled"),
    AWAITING_RESULT("awaiting_result"),
    DONE("done");

    private final String dbValue;

    TournamentMatchStatus(final String dbValue) {
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
