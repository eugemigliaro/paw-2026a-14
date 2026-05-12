package ar.edu.itba.paw.models.types;

public enum ParticipantStatus implements PersistableEnum {
    INVITED("invited"),
    JOINED("joined"),
    WAITLISTED("waitlisted"),
    CANCELLED("cancelled"),
    CHECKED_IN("checked_in"),
    PENDING_APPROVAL("pending_approval"),
    DECLINED_INVITE("declined_invite");

    private final String dbValue;

    ParticipantStatus(String dbValue) {
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
