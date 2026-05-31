package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchNotificationServiceImpl implements MatchNotificationService {

    private final MatchParticipantDao matchParticipantDao;
    private final MailDispatchService mailDispatchService;

    @Autowired
    public MatchNotificationServiceImpl(
            final MatchParticipantDao matchParticipantDao,
            final MailDispatchService mailDispatchService) {
        this.matchParticipantDao = matchParticipantDao;
        this.mailDispatchService = mailDispatchService;
    }

    @Override
    public void notifyMatchUpdated(final Match match) {
        final List<User> participants =
                matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            mailDispatchService.sendMatchUpdated(participant, match);
        }
    }

    @Override
    public void notifyMatchCancelled(final Match match) {
        final List<User> participants =
                matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            mailDispatchService.sendMatchCancelled(participant, match);
        }
    }

    @Override
    public void notifyRecurringMatchesUpdated(final List<Match> matches) {
        final List<Match> safeMatches = safeMatches(matches);
        if (safeMatches.isEmpty()) {
            return;
        }

        for (final AffectedRecurringParticipant participant :
                affectedParticipants(safeMatches).values()) {
            mailDispatchService.sendRecurringMatchesUpdated(
                    participant.user(),
                    participant.firstAffectedMatch(),
                    participant.affectedOccurrenceCount());
        }
    }

    @Override
    public void notifyRecurringMatchesCancelled(final List<Match> matches) {
        final List<Match> safeMatches = safeMatches(matches);
        if (safeMatches.isEmpty()) {
            return;
        }

        for (final AffectedRecurringParticipant participant :
                affectedParticipants(safeMatches).values()) {
            mailDispatchService.sendRecurringMatchesCancelled(
                    participant.user(),
                    participant.firstAffectedMatch(),
                    participant.affectedOccurrenceCount());
        }
    }

    @Override
    public void notifyHostPlayerJoined(final Match match, final User player) {
        final User host = match.getHost();

        if (host == null) {
            return;
        }

        mailDispatchService.sendPlayerJoined(host, match, player);
    }

    @Override
    public void notifyHostJoinRequestReceived(final Match match, final User player) {
        final User host = match.getHost();

        if (host == null) {
            return;
        }

        mailDispatchService.sendJoinRequestReceived(host, match, player);
    }

    @Override
    public void notifyPlayerRequestApproved(final Match match, final User player) {
        mailDispatchService.sendJoinRequestApproved(player, match);
    }

    @Override
    public void notifyPlayerRequestRejected(final Match match, final User player) {
        mailDispatchService.sendJoinRequestRejected(player, match);
    }

    @Override
    public void notifyPendingRequestClosedByPrivacyChange(
            final Match match, final List<User> players) {
        for (final User player : safeUsers(players)) {
            mailDispatchService.sendPendingRequestClosedByPrivacyChange(player, match);
        }
    }

    @Override
    public void notifyInvitationOpenedToPublic(final Match match, final List<User> players) {
        for (final User player : safeUsers(players)) {
            mailDispatchService.sendInvitationOpenedToPublic(player, match);
        }
    }

    @Override
    public void notifyHostInviteAccepted(final Match match, final User player) {
        final User host = match.getHost();

        if (host == null) {
            return;
        }

        mailDispatchService.sendInviteAccepted(host, match, player);
    }

    @Override
    public void notifyHostInviteDeclined(final Match match, final User player) {
        final User host = match.getHost();

        if (host == null) {
            return;
        }

        mailDispatchService.sendInviteDeclined(host, match, player);
    }

    @Override
    public void notifyHostPlayerLeft(final Match match, final User player) {
        final User host = match.getHost();

        if (host == null) {
            return;
        }

        mailDispatchService.sendPlayerLeft(host, match, player);
    }

    @Override
    public void notifyPlayerRemovedByHost(final Match match, final User player) {
        mailDispatchService.sendPlayerRemoved(player, match);
    }

    private Map<String, AffectedRecurringParticipant> affectedParticipants(
            final List<Match> matches) {
        final Map<String, AffectedRecurringParticipant> participantsByIdentity =
                new LinkedHashMap<>();
        for (final Match match : matches) {
            for (final User participant :
                    matchParticipantDao.findConfirmedParticipants(match.getId())) {
                final String identity = participantIdentity(participant);
                participantsByIdentity.compute(
                        identity,
                        (ignored, existing) ->
                                existing == null
                                        ? new AffectedRecurringParticipant(participant, match, 1)
                                        : existing.incremented());
            }
        }
        return participantsByIdentity;
    }

    private static String participantIdentity(final User participant) {
        if (participant.getId() != null) {
            return "id:" + participant.getId();
        }
        return "email:" + participant.getEmail().trim().toLowerCase(Locale.ROOT);
    }

    private static List<Match> safeMatches(final List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        return matches.stream().filter(match -> match != null).toList();
    }

    private static List<User> safeUsers(final List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream().filter(user -> user != null && user.getEmail() != null).toList();
    }

    private record AffectedRecurringParticipant(
            User user, Match firstAffectedMatch, int affectedOccurrenceCount) {

        private AffectedRecurringParticipant incremented() {
            return new AffectedRecurringParticipant(
                    user, firstAffectedMatch, affectedOccurrenceCount + 1);
        }
    }
}
