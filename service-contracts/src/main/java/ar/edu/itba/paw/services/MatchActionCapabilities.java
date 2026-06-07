package ar.edu.itba.paw.services;

public class MatchActionCapabilities {

    private final boolean visible;
    private final boolean canEdit;
    private final boolean canCancel;
    private final boolean canManageParticipants;
    private final boolean canReserve;
    private final boolean canCancelReservation;
    private final boolean canRequestToJoin;
    private final boolean canEditSeries;
    private final boolean canCancelSeries;

    public MatchActionCapabilities(
            final boolean visible,
            final boolean canEdit,
            final boolean canCancel,
            final boolean canManageParticipants,
            final boolean canReserve,
            final boolean canCancelReservation,
            final boolean canRequestToJoin,
            final boolean canEditSeries,
            final boolean canCancelSeries) {
        this.visible = visible;
        this.canEdit = canEdit;
        this.canCancel = canCancel;
        this.canManageParticipants = canManageParticipants;
        this.canReserve = canReserve;
        this.canCancelReservation = canCancelReservation;
        this.canRequestToJoin = canRequestToJoin;
        this.canEditSeries = canEditSeries;
        this.canCancelSeries = canCancelSeries;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public boolean isCanCancel() {
        return canCancel;
    }

    public boolean isCanManageParticipants() {
        return canManageParticipants;
    }

    public boolean isCanReserve() {
        return canReserve;
    }

    public boolean isCanCancelReservation() {
        return canCancelReservation;
    }

    public boolean isCanRequestToJoin() {
        return canRequestToJoin;
    }

    public boolean isCanEditSeries() {
        return canEditSeries;
    }

    public boolean isCanCancelSeries() {
        return canCancelSeries;
    }
}
