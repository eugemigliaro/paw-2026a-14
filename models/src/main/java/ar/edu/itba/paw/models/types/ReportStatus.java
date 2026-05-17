package ar.edu.itba.paw.models.types;

public enum ReportStatus implements PersistableEnum {
    PENDING("pending"),
    UNDER_REVIEW("under_review"),
    RESOLVED("resolved"),
    APPEALED("appealed"),
    FINALIZED("finalized");

    private final String dbValue;

    ReportStatus(final String dbValue) {
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
