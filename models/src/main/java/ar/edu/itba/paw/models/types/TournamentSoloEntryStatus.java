package ar.edu.itba.paw.models.types;

public enum TournamentSoloEntryStatus implements PersistableEnum {
    IN_POOL("in_pool"),
    LEFT("left"),
    ASSIGNED("assigned"),
    UNASSIGNED("unassigned");

    private final String dbValue;

    TournamentSoloEntryStatus(final String dbValue) {
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
