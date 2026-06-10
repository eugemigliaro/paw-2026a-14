package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import java.util.ArrayList;
import java.util.List;

/** Hand-rolled fake that records notification attempts for state-based test assertions. */
class RecordingMatchNotificationService implements MatchNotificationService {

    final List<String> actions = new ArrayList<>();
    final List<List<User>> affectedUserGroups = new ArrayList<>();
    final List<User> approvedUsers = new ArrayList<>();

    @Override
    public void notifyMatchUpdated(final Match match) {
        actions.add("match-updated");
    }

    @Override
    public void notifyMatchCancelled(final Match match) {
        actions.add("match-cancelled");
    }

    @Override
    public void notifyRecurringMatchesUpdated(final List<Match> matches) {
        actions.add("recurring-matches-updated");
    }

    @Override
    public void notifyRecurringMatchesCancelled(final List<Match> matches) {
        actions.add("recurring-matches-cancelled");
    }

    @Override
    public void notifyHostPlayerJoined(final Match match, final User player) {
        actions.add("host-player-joined");
    }

    @Override
    public void notifyHostJoinRequestReceived(final Match match, final User player) {
        actions.add("host-join-request-received");
    }

    @Override
    public void notifyPlayerRequestApproved(final Match match, final User player) {
        actions.add("request-approved");
        approvedUsers.add(player);
    }

    @Override
    public void notifyPlayerRequestRejected(final Match match, final User player) {
        actions.add("request-rejected");
    }

    @Override
    public void notifyPendingRequestClosedByPrivacyChange(
            final Match match, final List<User> players) {
        actions.add("pending-request-closed");
        affectedUserGroups.add(players);
    }

    @Override
    public void notifyInvitationOpenedToPublic(final Match match, final List<User> players) {
        actions.add("invitation-opened-to-public");
        affectedUserGroups.add(players);
    }

    @Override
    public void notifyHostInviteAccepted(final Match match, final User player) {
        actions.add("host-invite-accepted");
    }

    @Override
    public void notifyHostInviteDeclined(final Match match, final User player) {
        actions.add("host-invite-declined");
    }

    @Override
    public void notifyHostPlayerLeft(final Match match, final User player) {
        actions.add("host-player-left");
    }

    @Override
    public void notifyPlayerRemovedByHost(final Match match, final User player) {
        actions.add("player-removed-by-host");
    }
}
