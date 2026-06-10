package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import java.util.List;
import java.util.Optional;

public interface TournamentRegistrationService {

    TournamentSoloEntry joinSolo(long tournamentId, User user);

    void leaveSolo(long tournamentId, User user);

    TournamentTeam createTeam(long tournamentId, User user, String name);

    TournamentTeamMember joinTeam(long tournamentId, long teamId, User user);

    void leaveTeam(long tournamentId, User user);

    List<TournamentTeam> listJoinableTeams(long tournamentId);

    void withdrawFromOpenRegistrations(User user);

    boolean isSoloPoolFull(long tournamentId);

    Optional<TournamentSoloEntry> findSoloEntry(long tournamentId, User user);

    Optional<TournamentTeam> findUserTeam(long tournamentId, User user);

    List<TournamentSoloEntry> listActiveSoloEntries(long tournamentId);

    List<TournamentTeamMember> listTeamMembers(long tournamentId);

    TournamentRegistrationState getRegistrationState(
            Tournament tournament, User user, boolean canCloseRegistration);

    TournamentRegistrationReadiness getRegistrationReadiness(long tournamentId);

    Tournament closeRegistration(long tournamentId);
}
