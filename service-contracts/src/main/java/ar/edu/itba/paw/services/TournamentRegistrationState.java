package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import java.util.Optional;

public class TournamentRegistrationState {

    private final Optional<TournamentSoloEntry> soloEntry;
    private final Optional<TournamentTeam> userTeam;
    private final TournamentRegistrationReadiness readiness;
    private final boolean registrationOpen;
    private final boolean registrationNotStarted;
    private final boolean canJoinSolo;
    private final boolean canLeaveSolo;
    private final boolean requiresLoginToJoin;
    private final boolean closeRegistrationDisabled;

    public TournamentRegistrationState(
            final Optional<TournamentSoloEntry> soloEntry,
            final Optional<TournamentTeam> userTeam,
            final TournamentRegistrationReadiness readiness,
            final boolean registrationOpen,
            final boolean registrationNotStarted,
            final boolean canJoinSolo,
            final boolean canLeaveSolo,
            final boolean requiresLoginToJoin,
            final boolean closeRegistrationDisabled) {
        this.soloEntry = soloEntry == null ? Optional.empty() : soloEntry;
        this.userTeam = userTeam == null ? Optional.empty() : userTeam;
        this.readiness = readiness;
        this.registrationOpen = registrationOpen;
        this.registrationNotStarted = registrationNotStarted;
        this.canJoinSolo = canJoinSolo;
        this.canLeaveSolo = canLeaveSolo;
        this.requiresLoginToJoin = requiresLoginToJoin;
        this.closeRegistrationDisabled = closeRegistrationDisabled;
    }

    public Optional<TournamentSoloEntry> getSoloEntry() {
        return soloEntry;
    }

    public Optional<TournamentTeam> getUserTeam() {
        return userTeam;
    }

    public TournamentRegistrationReadiness getReadiness() {
        return readiness;
    }

    public boolean isRegistrationOpen() {
        return registrationOpen;
    }

    public boolean isRegistrationNotStarted() {
        return registrationNotStarted;
    }

    public boolean canJoinSolo() {
        return canJoinSolo;
    }

    public boolean canLeaveSolo() {
        return canLeaveSolo;
    }

    public boolean requiresLoginToJoin() {
        return requiresLoginToJoin;
    }

    public boolean isCloseRegistrationDisabled() {
        return closeRegistrationDisabled;
    }
}
