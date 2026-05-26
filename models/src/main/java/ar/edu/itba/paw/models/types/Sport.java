package ar.edu.itba.paw.models.types;

public enum Sport implements PersistableEnum {
    FOOTBALL("football", "Football"),
    TENNIS("tennis", "Tennis"),
    PADEL("padel", "Padel"),
    BASKETBALL("basketball", "Basketball"),
    OTHER("other", "Other");

    private final String dbValue;
    private final String displayName;

    Sport(final String dbValue, final String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    @Override
    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return dbValue;
    }
}
