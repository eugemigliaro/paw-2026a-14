package ar.edu.itba.paw.services;

public enum MatchUpdateFailureReason {
    MATCH_NOT_FOUND,
    FORBIDDEN,
    NOT_EDITABLE,
    NOT_RECURRING,
    INVALID_SCHEDULE,
    CAPACITY_BELOW_CONFIRMED
}
