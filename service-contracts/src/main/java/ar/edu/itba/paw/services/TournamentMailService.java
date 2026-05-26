package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;

public interface TournamentMailService {

    void sendBracketPublishedEmail(Tournament tournament);

    void sendMatchResultEmail(
            Tournament tournament,
            TournamentMatch match,
            TournamentTeam winner,
            TournamentTeam loser);

    void sendTournamentCompletedEmail(Tournament tournament, TournamentTeam champion);

    void sendTournamentCancelledEmail(Tournament tournament);
}
