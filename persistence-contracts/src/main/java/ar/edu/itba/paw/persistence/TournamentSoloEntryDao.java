package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentSoloEntry;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.TournamentSoloEntryStatus;
import java.util.List;
import java.util.Optional;

public interface TournamentSoloEntryDao {

    TournamentSoloEntry create(Tournament tournament, User user, TournamentSoloEntryStatus status);

    Optional<TournamentSoloEntry> findByTournamentAndUser(long tournamentId, long userId);

    List<Tournament> findTournamentsByUser(User user);

    List<TournamentSoloEntry> findActiveByTournament(long tournamentId);

    long countActiveByTournament(long tournamentId);

    TournamentSoloEntry update(TournamentSoloEntry soloEntry);
}
