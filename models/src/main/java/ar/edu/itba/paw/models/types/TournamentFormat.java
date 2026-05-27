package ar.edu.itba.paw.models.types;

public enum TournamentFormat implements PersistableEnum {
    SINGLE_ELIMINATION("single_elimination");

    private final String dbValue;

    TournamentFormat(final String dbValue) {
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
