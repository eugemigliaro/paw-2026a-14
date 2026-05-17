package ar.edu.itba.paw.models.types;

public enum PlayerReviewReaction implements PersistableEnum {
    LIKE("like"),
    DISLIKE("dislike");

    private final String dbValue;

    PlayerReviewReaction(final String dbValue) {
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
