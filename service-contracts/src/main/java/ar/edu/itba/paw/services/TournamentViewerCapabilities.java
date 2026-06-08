package ar.edu.itba.paw.services;

public class TournamentViewerCapabilities {

    private final boolean canJoinSolo;
    private final boolean canLeaveSolo;
    private final boolean requiresLoginToJoin;
    private final boolean registrationNotStarted;
    private final boolean canCloseRegistration;
    private final boolean canEditTournament;
    private final boolean canCancelTournament;
    private final boolean canManageBracket;
    private final boolean canManageResults;
    private final boolean canViewBracket;
    private final boolean closeRegistrationDisabled;
    private final boolean closeRegistrationBlockedByCapacity;

    public TournamentViewerCapabilities(
            final boolean canJoinSolo,
            final boolean canLeaveSolo,
            final boolean requiresLoginToJoin,
            final boolean registrationNotStarted,
            final boolean canCloseRegistration,
            final boolean canEditTournament,
            final boolean canCancelTournament,
            final boolean canManageBracket,
            final boolean canManageResults,
            final boolean canViewBracket,
            final boolean closeRegistrationDisabled,
            final boolean closeRegistrationBlockedByCapacity) {
        this.canJoinSolo = canJoinSolo;
        this.canLeaveSolo = canLeaveSolo;
        this.requiresLoginToJoin = requiresLoginToJoin;
        this.registrationNotStarted = registrationNotStarted;
        this.canCloseRegistration = canCloseRegistration;
        this.canEditTournament = canEditTournament;
        this.canCancelTournament = canCancelTournament;
        this.canManageBracket = canManageBracket;
        this.canManageResults = canManageResults;
        this.canViewBracket = canViewBracket;
        this.closeRegistrationDisabled = closeRegistrationDisabled;
        this.closeRegistrationBlockedByCapacity = closeRegistrationBlockedByCapacity;
    }

    public boolean isCanJoinSolo() {
        return canJoinSolo;
    }

    public boolean isCanLeaveSolo() {
        return canLeaveSolo;
    }

    public boolean isRequiresLoginToJoin() {
        return requiresLoginToJoin;
    }

    public boolean isRegistrationNotStarted() {
        return registrationNotStarted;
    }

    public boolean isCanCloseRegistration() {
        return canCloseRegistration;
    }

    public boolean isCanEditTournament() {
        return canEditTournament;
    }

    public boolean isCanCancelTournament() {
        return canCancelTournament;
    }

    public boolean isCanManageBracket() {
        return canManageBracket;
    }

    public boolean isCanManageResults() {
        return canManageResults;
    }

    public boolean isCanViewBracket() {
        return canViewBracket;
    }

    public boolean isCloseRegistrationDisabled() {
        return closeRegistrationDisabled;
    }

    public boolean isCloseRegistrationBlockedByCapacity() {
        return closeRegistrationBlockedByCapacity;
    }
}
