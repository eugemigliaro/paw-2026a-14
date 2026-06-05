package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import java.time.Instant;
import java.util.Locale;

public interface MailDispatchService {

    default void sendAccountVerification(
            final UserAccount account,
            final String confirmationUrl,
            final Instant expiresAt,
            final Locale locale) {}

    default void sendPasswordReset(
            final UserAccount account,
            final String resetUrl,
            final Instant expiresAt,
            final Locale locale) {}

    default void sendMatchUpdated(final User recipient, final Match match) {}

    default void sendMatchCancelled(final User recipient, final Match match) {}

    default void sendRecurringMatchesUpdated(
            final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {}

    default void sendRecurringMatchesCancelled(
            final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {}

    default void sendMatchInvitation(final User recipient, final Match match) {}

    default void sendSeriesInvitation(
            final User recipient, final Match match, final int occurrenceCount) {}

    default void sendJoinRequestReceived(final User host, final Match match, final User player) {}

    default void sendJoinRequestApproved(final User player, final Match match) {}

    default void sendJoinRequestRejected(final User player, final Match match) {}

    default void sendPendingRequestClosedByPrivacyChange(final User player, final Match match) {}

    default void sendInvitationOpenedToPublic(final User player, final Match match) {}

    default void sendInviteAccepted(final User host, final Match match, final User player) {}

    default void sendInviteDeclined(final User host, final Match match, final User player) {}

    default void sendPlayerJoined(final User host, final Match match, final User player) {}

    default void sendPlayerLeft(final User host, final Match match, final User player) {}

    default void sendPlayerRemoved(final User player, final Match match) {}

    default void sendTournamentBracketPublished(
            final User recipient, final Tournament tournament) {}

    default void sendTournamentMatchResult(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser) {}

    default void sendTournamentCompleted(
            final User recipient, final Tournament tournament, final TournamentTeam champion) {}

    default void sendTournamentCancelled(final User recipient, final Tournament tournament) {}

    default void sendBanNotice(final User user, final Instant bannedUntil, final String reason) {}

    default void sendUnbanNotice(final User user) {}
}
