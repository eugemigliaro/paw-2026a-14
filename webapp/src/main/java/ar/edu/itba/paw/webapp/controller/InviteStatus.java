package ar.edu.itba.paw.webapp.controller;

public enum InviteStatus {
    ACCEPTED("accepted");

    private final String code;

    InviteStatus(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
