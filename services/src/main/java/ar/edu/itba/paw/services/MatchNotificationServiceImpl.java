package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.MatchParticipantDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.MatchLifecycleMailTemplateData;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MatchNotificationServiceImpl implements MatchNotificationService {

    private final MatchParticipantDao matchParticipantDao;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
    private final UserService userService;

    @Autowired
    public MatchNotificationServiceImpl(
            final MatchParticipantDao matchParticipantDao,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource,
            final UserService userService) {
        this.matchParticipantDao = matchParticipantDao;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
        this.userService = userService;
    }

    @Override
    public void notifyMatchUpdated(final Match match) {
        final List<User> participants =
                matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant, match, null);
            final MailContent content =
                    templateRenderer.renderMatchUpdatedNotification(templateData);
            mailDispatchService.dispatch(participant.getEmail(), content);
        }
    }

    @Override
    public void notifyMatchCancelled(final Match match) {
        final List<User> participants =
                matchParticipantDao.findConfirmedParticipants(match.getId());
        for (final User participant : participants) {
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant, match, null);
            final MailContent content =
                    templateRenderer.renderMatchCancelledNotification(templateData);
            mailDispatchService.dispatch(participant.getEmail(), content);
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
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant.user(), participant.firstAffectedMatch(), null);
            final MailContent content =
                    templateRenderer.renderRecurringMatchesUpdatedNotification(
                            templateData, participant.affectedOccurrenceCount());
            mailDispatchService.dispatch(participant.user().getEmail(), content);
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
            final MatchLifecycleMailTemplateData templateData =
                    buildTemplateData(participant.user(), participant.firstAffectedMatch(), null);
            final MailContent content =
                    templateRenderer.renderRecurringMatchesCancelledNotification(
                            templateData, participant.affectedOccurrenceCount());
            mailDispatchService.dispatch(participant.user().getEmail(), content);
        }
    }

    @Override
    public void notifyHostPlayerJoined(final Match match, final User player) {
        userService
                .findById(match.getHostUserId())
                .ifPresent(
                        host -> {
                            final MatchLifecycleMailTemplateData templateData =
                                    buildTemplateData(
                                            host,
                                            match,
                                            player.getName() + " " + player.getLastName());
                            final MailContent content =
                                    templateRenderer.renderPlayerJoinedNotification(templateData);
                            mailDispatchService.dispatch(host.getEmail(), content);
                        });
    }

    @Override
    public void notifyHostJoinRequestReceived(final Match match, final User player) {
        userService
                .findById(match.getHostUserId())
                .ifPresent(
                        host -> {
                            final MatchLifecycleMailTemplateData templateData =
                                    buildTemplateData(
                                            host,
                                            match,
                                            player.getName() + " " + player.getLastName());
                            final MailContent content =
                                    templateRenderer.renderJoinRequestReceivedNotification(
                                            templateData);
                            mailDispatchService.dispatch(host.getEmail(), content);
                        });
    }

    @Override
    public void notifyPlayerRequestApproved(final Match match, final User player) {
        final MatchLifecycleMailTemplateData templateData = buildTemplateData(player, match, null);
        final MailContent content =
                templateRenderer.renderJoinRequestApprovedNotification(templateData);
        mailDispatchService.dispatch(player.getEmail(), content);
    }

    @Override
    public void notifyPlayerRequestRejected(final Match match, final User player) {
        final MatchLifecycleMailTemplateData templateData = buildTemplateData(player, match, null);
        final MailContent content =
                templateRenderer.renderJoinRequestRejectedNotification(templateData);
        mailDispatchService.dispatch(player.getEmail(), content);
    }

    @Override
    public void notifyHostInviteAccepted(final Match match, final User player) {
        userService
                .findById(match.getHostUserId())
                .ifPresent(
                        host -> {
                            final MatchLifecycleMailTemplateData templateData =
                                    buildTemplateData(
                                            host,
                                            match,
                                            player.getName() + " " + player.getLastName());
                            final MailContent content =
                                    templateRenderer.renderInviteAcceptedNotification(templateData);
                            mailDispatchService.dispatch(host.getEmail(), content);
                        });
    }

    @Override
    public void notifyHostInviteDeclined(final Match match, final User player) {
        userService
                .findById(match.getHostUserId())
                .ifPresent(
                        host -> {
                            final MatchLifecycleMailTemplateData templateData =
                                    buildTemplateData(
                                            host,
                                            match,
                                            player.getName() + " " + player.getLastName());
                            final MailContent content =
                                    templateRenderer.renderInviteDeclinedNotification(templateData);
                            mailDispatchService.dispatch(host.getEmail(), content);
                        });
    }

    @Override
    public void notifyHostPlayerLeft(final Match match, final User player) {
        userService
                .findById(match.getHostUserId())
                .ifPresent(
                        host -> {
                            final MatchLifecycleMailTemplateData templateData =
                                    buildTemplateData(
                                            host,
                                            match,
                                            player.getName() + " " + player.getLastName());
                            final MailContent content =
                                    templateRenderer.renderPlayerLeftNotification(templateData);
                            mailDispatchService.dispatch(host.getEmail(), content);
                        });
    }

    @Override
    public void notifyPlayerRemovedByHost(final Match match, final User player) {
        final MatchLifecycleMailTemplateData templateData = buildTemplateData(player, match, null);
        final MailContent content =
                templateRenderer.renderParticipantRemovedNotification(templateData);
        mailDispatchService.dispatch(player.getEmail(), content);
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

    private record AffectedRecurringParticipant(
            User user, Match firstAffectedMatch, int affectedOccurrenceCount) {

        private AffectedRecurringParticipant incremented() {
            return new AffectedRecurringParticipant(
                    user, firstAffectedMatch, affectedOccurrenceCount + 1);
        }
    }

    private MatchLifecycleMailTemplateData buildTemplateData(
            final User recipient, final Match match, final String actorName) {
        final Locale locale = LocaleContextHolder.getLocale();
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "match.status." + match.getStatus(), null, match.getStatus(), locale);

        return new MatchLifecycleMailTemplateData(
                recipient.getEmail(),
                match.getTitle(),
                match.getAddress(),
                match.getStartsAt(),
                match.getEndsAt(),
                sportLabel,
                statusLabel,
                actorName,
                locale);
    }
}
