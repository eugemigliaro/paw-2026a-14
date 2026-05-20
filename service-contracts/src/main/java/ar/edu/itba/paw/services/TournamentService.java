package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.User;
import java.util.Optional;

public interface TournamentService {

    Tournament createDraft(User host, CreateTournamentRequest request);

    Tournament publish(long tournamentId, User actingUser);

    Optional<Tournament> findPublicTournament(long tournamentId);

    Optional<Tournament> findTournamentForHost(long tournamentId, User host);

    Tournament update(long tournamentId, User actingUser, UpdateTournamentRequest request);

    Tournament cancel(long tournamentId, User actingUser, String reason);
}
