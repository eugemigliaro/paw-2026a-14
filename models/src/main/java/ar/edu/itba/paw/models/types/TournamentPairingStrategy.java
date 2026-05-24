package ar.edu.itba.paw.models.types;

public enum TournamentPairingStrategy implements PersistableEnum {
    MANUAL("manual"),
    RANDOM("random"),
    ELO("elo");

    private final String dbValue;

    TournamentPairingStrategy(final String dbValue) {
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
