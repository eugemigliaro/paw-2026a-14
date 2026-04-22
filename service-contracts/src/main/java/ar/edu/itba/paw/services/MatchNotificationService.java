package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;

public interface MatchNotificationService {

    void notifyMatchUpdated(Match match);

    void notifyMatchCancelled(Match match);
}
