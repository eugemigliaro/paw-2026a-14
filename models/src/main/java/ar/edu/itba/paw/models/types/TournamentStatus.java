package ar.edu.itba.paw.models.types;

public enum TournamentStatus implements PersistableEnum {
    DRAFT("draft"),
    REGISTRATION("registration"),
    BRACKET_SETUP("bracket_setup"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    private final String dbValue;

    TournamentStatus(final String dbValue) {
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
