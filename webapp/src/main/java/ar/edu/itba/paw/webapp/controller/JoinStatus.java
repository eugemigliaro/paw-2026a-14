package ar.edu.itba.paw.webapp.controller;

public enum JoinStatus {
    REQUESTED("requested"),
    RECURRING_REQUESTED("recurringRequested"),
    CANCELLED("cancelled");

    private final String code;

    JoinStatus(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
