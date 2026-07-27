package ar.edu.itba.paw.webapp.controller;

public enum HostAction {
    UPDATED("updated", HostActionTarget.PARTICIPANTS),
    SERIES_UPDATED("seriesUpdated", HostActionTarget.PARTICIPANTS),
    CANCELLED("cancelled", HostActionTarget.PARTICIPANTS),
    SERIES_CANCELLED("seriesCancelled", HostActionTarget.PARTICIPANTS),
    PARTICIPANT_REMOVED("participantRemoved", HostActionTarget.PARTICIPANTS),
    REQUEST_APPROVED("requestApproved", HostActionTarget.REQUESTS),
    REQUEST_REJECTED("requestRejected", HostActionTarget.REQUESTS),
    INVITE_SENT("inviteSent", HostActionTarget.INVITES),
    SERIES_INVITE_SENT("seriesInviteSent", HostActionTarget.INVITES);

    private final String code;
    private final HostActionTarget target;

    HostAction(final String code, final HostActionTarget target) {
        this.code = code;
        this.target = target;
    }

    public String getCode() {
        return code;
    }

    boolean targetsRequests() {
        return target == HostActionTarget.REQUESTS;
    }

    boolean targetsInvites() {
        return target == HostActionTarget.INVITES;
    }
}
