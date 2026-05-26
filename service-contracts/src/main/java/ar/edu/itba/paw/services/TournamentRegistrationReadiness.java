package ar.edu.itba.paw.services;

public class TournamentRegistrationReadiness {

    private final int activeSoloEntries;
    private final int existingTeamCount;
    private final int finalTeamCount;
    private final boolean cancellationRisk;

    public TournamentRegistrationReadiness(
            final int activeSoloEntries,
            final int existingTeamCount,
            final int finalTeamCount,
            final boolean cancellationRisk) {
        this.activeSoloEntries = activeSoloEntries;
        this.existingTeamCount = existingTeamCount;
        this.finalTeamCount = finalTeamCount;
        this.cancellationRisk = cancellationRisk;
    }

    public int getActiveSoloEntries() {
        return activeSoloEntries;
    }

    public int getExistingTeamCount() {
        return existingTeamCount;
    }

    public int getFinalTeamCount() {
        return finalTeamCount;
    }

    public boolean isCancellationRisk() {
        return cancellationRisk;
    }
}
