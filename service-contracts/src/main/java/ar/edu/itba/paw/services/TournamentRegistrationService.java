package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface TournamentRegistrationService {

    TournamentSoloEntry joinSolo(long tournamentId, User user);

    void leaveSolo(long tournamentId, User user);

    boolean isSoloPoolFull(long tournamentId);

    Optional<TournamentSoloEntry> findSoloEntry(long tournamentId, User user);

    Optional<TournamentTeam> findUserTeam(long tournamentId, User user);

    Tournament closeRegistration(long tournamentId, User actingUser);
}
