package ar.edu.itba.paw.models.types;

public enum TournamentTeamOrigin implements PersistableEnum {
    SOLO_POOL("solo_pool"),
    TEAM_DRAFT("team_draft");

    private final String dbValue;

    TournamentTeamOrigin(final String dbValue) {
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
