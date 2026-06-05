package ar.edu.itba.paw.services;

public class MatchManagementPermissions {

    private final boolean hostViewer;
    private final boolean canManage;
    private final boolean canManageParticipants;
    private final boolean canEdit;
    private final boolean canCancel;
    private final boolean canEditSeries;
    private final boolean canCancelSeries;

    public MatchManagementPermissions(
            final boolean hostViewer,
            final boolean canManage,
            final boolean canManageParticipants,
            final boolean canEdit,
            final boolean canCancel,
            final boolean canEditSeries,
            final boolean canCancelSeries) {
        this.hostViewer = hostViewer;
        this.canManage = canManage;
        this.canManageParticipants = canManageParticipants;
        this.canEdit = canEdit;
        this.canCancel = canCancel;
        this.canEditSeries = canEditSeries;
        this.canCancelSeries = canCancelSeries;
    }

    public static MatchManagementPermissions none() {
        return new MatchManagementPermissions(false, false, false, false, false, false, false);
    }

    public boolean isHostViewer() {
        return hostViewer;
    }

    public boolean canManage() {
        return canManage;
    }

    public boolean canManageParticipants() {
        return canManageParticipants;
    }

    public boolean canEdit() {
        return canEdit;
    }

    public boolean canCancel() {
        return canCancel;
    }

    public boolean canEditSeries() {
        return canEditSeries;
    }

    public boolean canCancelSeries() {
        return canCancelSeries;
    }
}
