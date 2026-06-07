package ar.edu.itba.paw.services;

public class TournamentManagementPermissions {

    private final boolean canCloseRegistration;
    private final boolean canEditTournament;
    private final boolean canCancelTournament;
    private final boolean canManageBracket;
    private final boolean canViewBracket;
    private final boolean canDefineMatchDates;
    private final boolean canManageResults;

    public TournamentManagementPermissions(
            final boolean canCloseRegistration,
            final boolean canEditTournament,
            final boolean canCancelTournament,
            final boolean canManageBracket,
            final boolean canViewBracket,
            final boolean canDefineMatchDates,
            final boolean canManageResults) {
        this.canCloseRegistration = canCloseRegistration;
        this.canEditTournament = canEditTournament;
        this.canCancelTournament = canCancelTournament;
        this.canManageBracket = canManageBracket;
        this.canViewBracket = canViewBracket;
        this.canDefineMatchDates = canDefineMatchDates;
        this.canManageResults = canManageResults;
    }

    public boolean canCloseRegistration() {
        return canCloseRegistration;
    }

    public boolean canEditTournament() {
        return canEditTournament;
    }

    public boolean canCancelTournament() {
        return canCancelTournament;
    }

    public boolean canManageBracket() {
        return canManageBracket;
    }

    public boolean canViewBracket() {
        return canViewBracket;
    }

    public boolean canDefineMatchDates() {
        return canDefineMatchDates;
    }

    public boolean canManageResults() {
        return canManageResults;
    }
}
