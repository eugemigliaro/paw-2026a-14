package ar.edu.itba.paw.webapp.controller;

public enum HostActionTarget {
    REQUESTS("requests"),
    INVITES("invites"),
    PARTICIPANTS("participants");

    private final String code;

    HostActionTarget(final String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
