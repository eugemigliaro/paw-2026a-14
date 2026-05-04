package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import java.util.List;

public interface MatchNotificationService {

    void notifyMatchUpdated(Match match);

    void notifyMatchCancelled(Match match);

    void notifyRecurringMatchesUpdated(List<Match> matches);

    void notifyRecurringMatchesCancelled(List<Match> matches);

    void notifyHostPlayerJoined(Match match, User player);

    void notifyHostJoinRequestReceived(Match match, User player);

    void notifyPlayerRequestApproved(Match match, User player);

    void notifyPlayerRequestRejected(Match match, User player);

    void notifyPendingRequestClosedByPrivacyChange(Match match, List<User> players);

    void notifyInvitationOpenedToPublic(Match match, List<User> players);

    void notifyHostInviteAccepted(Match match, User player);

    void notifyHostInviteDeclined(Match match, User player);

    void notifyHostPlayerLeft(Match match, User player);

    void notifyPlayerRemovedByHost(Match match, User player);
}
