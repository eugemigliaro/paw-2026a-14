package ar.edu.itba.paw.webapp.controller;

import java.util.Arrays;
import java.util.Optional;

enum HostAction {
    UPDATED("updated", Target.GENERAL),
    SERIES_UPDATED("seriesUpdated", Target.GENERAL),
    CANCELLED("cancelled", Target.GENERAL),
    SERIES_CANCELLED("seriesCancelled", Target.GENERAL),
    PARTICIPANT_REMOVED("participantRemoved", Target.GENERAL),
    REQUEST_APPROVED("requestApproved", Target.REQUESTS),
    REQUEST_REJECTED("requestRejected", Target.REQUESTS),
    INVITE_SENT("inviteSent", Target.INVITES),
    SERIES_INVITE_SENT("seriesInviteSent", Target.INVITES);

    private final String code;
    private final Target target;

    HostAction(final String code, final Target target) {
        this.code = code;
        this.target = target;
    }

    static Optional<HostAction> fromCode(final String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(action -> action.code.equalsIgnoreCase(code))
                .findFirst();
    }

    String getCode() {
        return code;
    }

    boolean targetsRequests() {
        return target == Target.REQUESTS;
    }

    boolean targetsInvites() {
        return target == Target.INVITES;
    }

    private enum Target {
        GENERAL,
        REQUESTS,
        INVITES
    }
}
