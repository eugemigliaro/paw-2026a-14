package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import java.util.List;

public interface MatchNotificationService {

    void notifyMatchUpdated(Match match);

    void notifyMatchCancelled(Match match);

    void notifyRecurringMatchesUpdated(List<Match> matches);

    void notifyRecurringMatchesCancelled(List<Match> matches);
}
