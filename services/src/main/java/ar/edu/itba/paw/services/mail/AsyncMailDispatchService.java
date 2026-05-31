package ar.edu.itba.paw.services.mail;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserAccount;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class AsyncMailDispatchService implements MailDispatchService {

    private final AsyncMailSender mailSender;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;
    private final MailProperties mailProperties;

    @Autowired
    public AsyncMailDispatchService(
            final AsyncMailSender mailSender,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource,
            final MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendAccountVerification(
            final UserAccount account,
            final String confirmationUrl,
            final Instant expiresAt,
            final Locale locale) {
        final Locale resolvedLocale = resolvedLocale(locale);
        dispatch(
                account.getEmail(),
                templateRenderer.renderActionMail(
                        new VerificationMailTemplateData(
                                message("verification.preview.account.title", resolvedLocale),
                                message("verification.preview.account.summary", resolvedLocale),
                                account.getEmail(),
                                confirmationUrl,
                                expiresAt,
                                List.of(),
                                resolvedLocale)));
    }

    @Override
    public void sendPasswordReset(
            final UserAccount account,
            final String resetUrl,
            final Instant expiresAt,
            final Locale locale) {
        final Locale resolvedLocale = resolvedLocale(locale);
        dispatch(
                account.getEmail(),
                templateRenderer.renderActionMail(
                        new VerificationMailTemplateData(
                                message("passwordReset.mail.title", resolvedLocale),
                                message("passwordReset.mail.summary", resolvedLocale),
                                account.getEmail(),
                                resetUrl,
                                expiresAt,
                                List.of(),
                                resolvedLocale)));
    }

    @Override
    public void sendMatchUpdated(final User recipient, final Match match) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderMatchUpdatedNotification(
                        buildMatchTemplateData(recipient, match, null)));
    }

    @Override
    public void sendMatchCancelled(final User recipient, final Match match) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderMatchCancelledNotification(
                        buildMatchTemplateData(recipient, match, null)));
    }

    @Override
    public void sendRecurringMatchesUpdated(
            final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderRecurringMatchesUpdatedNotification(
                        buildMatchTemplateData(recipient, firstAffectedMatch, null),
                        occurrenceCount));
    }

    @Override
    public void sendRecurringMatchesCancelled(
            final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderRecurringMatchesCancelledNotification(
                        buildMatchTemplateData(recipient, firstAffectedMatch, null),
                        occurrenceCount));
    }

    @Override
    public void sendMatchInvitation(final User recipient, final Match match) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderMatchInvitationNotification(
                        buildMatchTemplateData(recipient, match, null)));
    }

    @Override
    public void sendSeriesInvitation(
            final User recipient, final Match match, final int occurrenceCount) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderSeriesInvitationNotification(
                        buildMatchTemplateData(recipient, match, null), occurrenceCount));
    }

    @Override
    public void sendJoinRequestReceived(final User host, final Match match, final User player) {
        dispatch(
                host.getEmail(),
                templateRenderer.renderJoinRequestReceivedNotification(
                        buildMatchTemplateData(host, match, displayName(player))));
    }

    @Override
    public void sendJoinRequestApproved(final User player, final Match match) {
        dispatch(
                player.getEmail(),
                templateRenderer.renderJoinRequestApprovedNotification(
                        buildMatchTemplateData(player, match, null)));
    }

    @Override
    public void sendJoinRequestRejected(final User player, final Match match) {
        dispatch(
                player.getEmail(),
                templateRenderer.renderJoinRequestRejectedNotification(
                        buildMatchTemplateData(player, match, null)));
    }

    @Override
    public void sendPendingRequestClosedByPrivacyChange(final User player, final Match match) {
        dispatch(
                player.getEmail(),
                templateRenderer.renderPendingRequestClosedByPrivacyChangeNotification(
                        buildMatchTemplateData(player, match, null)));
    }

    @Override
    public void sendInvitationOpenedToPublic(final User player, final Match match) {
        dispatch(
                player.getEmail(),
                templateRenderer.renderInvitationOpenedToPublicNotification(
                        buildMatchTemplateData(player, match, null)));
    }

    @Override
    public void sendInviteAccepted(final User host, final Match match, final User player) {
        dispatch(
                host.getEmail(),
                templateRenderer.renderInviteAcceptedNotification(
                        buildMatchTemplateData(host, match, displayName(player))));
    }

    @Override
    public void sendInviteDeclined(final User host, final Match match, final User player) {
        dispatch(
                host.getEmail(),
                templateRenderer.renderInviteDeclinedNotification(
                        buildMatchTemplateData(host, match, displayName(player))));
    }

    @Override
    public void sendPlayerJoined(final User host, final Match match, final User player) {
        dispatch(
                host.getEmail(),
                templateRenderer.renderPlayerJoinedNotification(
                        buildMatchTemplateData(host, match, displayName(player))));
    }

    @Override
    public void sendPlayerLeft(final User host, final Match match, final User player) {
        dispatch(
                host.getEmail(),
                templateRenderer.renderPlayerLeftNotification(
                        buildMatchTemplateData(host, match, displayName(player))));
    }

    @Override
    public void sendPlayerRemoved(final User player, final Match match) {
        dispatch(
                player.getEmail(),
                templateRenderer.renderParticipantRemovedNotification(
                        buildMatchTemplateData(player, match, null)));
    }

    @Override
    public void sendTournamentBracketPublished(final User recipient, final Tournament tournament) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderTournamentBracketPublishedEmail(
                        buildTournamentBracketTemplateData(
                                recipient, tournament, null, null, null, null)));
    }

    @Override
    public void sendTournamentMatchResult(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderTournamentMatchResultEmail(
                        buildTournamentBracketTemplateData(
                                recipient, tournament, match, winner, loser, null)));
    }

    @Override
    public void sendTournamentCompleted(
            final User recipient, final Tournament tournament, final TournamentTeam champion) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderTournamentCompletedEmail(
                        buildTournamentDetailTemplateData(
                                recipient, tournament, null, null, null, champion)));
    }

    @Override
    public void sendTournamentCancelled(final User recipient, final Tournament tournament) {
        dispatch(
                recipient.getEmail(),
                templateRenderer.renderTournamentCancelledEmail(
                        buildTournamentDetailTemplateData(
                                recipient, tournament, null, null, null, null)));
    }

    @Override
    public void sendBanNotice(final User user, final Instant bannedUntil, final String reason) {
        final Locale locale = recipientLocale(user);
        dispatch(
                user.getEmail(),
                templateRenderer.renderBanNotification(
                        new BanMailTemplateData(
                                user.getEmail(),
                                user.getUsername(),
                                bannedUntil,
                                reason,
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale)));
    }

    @Override
    public void sendUnbanNotice(final User user) {
        final Locale locale = recipientLocale(user);
        dispatch(
                user.getEmail(),
                templateRenderer.renderUnbanNotification(
                        new UnbanMailTemplateData(
                                user.getEmail(),
                                user.getUsername(),
                                stripTrailingSlash(mailProperties.getBaseUrl()) + "/login",
                                locale)));
    }

    private void dispatch(final String recipientEmail, final MailContent content) {
        mailSender.send(recipientEmail, content);
    }

    private MatchLifecycleMailTemplateData buildMatchTemplateData(
            final User recipient, final Match match, final String actorName) {
        final Locale locale = recipientLocale(recipient);
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + match.getSport().getDbValue(),
                        null,
                        match.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "match.status." + match.getStatus().getValue(),
                        null,
                        match.getStatus().getValue(),
                        locale);

        return new MatchLifecycleMailTemplateData(
                recipient.getEmail(),
                match.getTitle(),
                match.getAddress(),
                match.getStartsAt(),
                match.getEndsAt(),
                sportLabel,
                statusLabel,
                actorName,
                appUrl("/matches/" + match.getId(), locale),
                locale);
    }

    private TournamentLifecycleMailTemplateData buildTournamentTemplateData(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser,
            final TournamentTeam champion,
            final boolean linkToBracket) {
        final Locale locale = recipientLocale(recipient);
        final String sportLabel =
                messageSource.getMessage(
                        "sport." + tournament.getSport().getDbValue(),
                        null,
                        tournament.getSport().getDisplayName(),
                        locale);
        final String statusLabel =
                messageSource.getMessage(
                        "tournament.status." + tournament.getStatus().getDbValue(),
                        null,
                        tournament.getStatus().getDbValue(),
                        locale);

        return new TournamentLifecycleMailTemplateData(
                recipient.getEmail(),
                tournament.getTitle(),
                sportLabel,
                statusLabel,
                matchLabel(match, locale),
                teamName(winner, locale),
                teamName(loser, locale),
                teamName(champion, locale),
                tournament.getAddress(),
                tournament.getStartsAt(),
                tournamentUrl(tournament, linkToBracket, locale),
                locale);
    }

    private TournamentLifecycleMailTemplateData buildTournamentBracketTemplateData(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser,
            final TournamentTeam champion) {
        return buildTournamentTemplateData(
                recipient, tournament, match, winner, loser, champion, true);
    }

    private TournamentLifecycleMailTemplateData buildTournamentDetailTemplateData(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser,
            final TournamentTeam champion) {
        return buildTournamentTemplateData(
                recipient, tournament, match, winner, loser, champion, false);
    }

    private String tournamentUrl(
            final Tournament tournament, final boolean linkToBracket, final Locale locale) {
        final String detailPath = "/tournaments/" + tournament.getId();
        return appUrl(linkToBracket ? detailPath + "/bracket" : detailPath, locale);
    }

    private String appUrl(final String path, final Locale locale) {
        final String baseUrl = stripTrailingSlash(mailProperties.getBaseUrl());
        final String languageTag = resolvedLocale(locale).getLanguage();
        return languageTag.isBlank() ? baseUrl + path : baseUrl + path + "?lang=" + languageTag;
    }

    private String matchLabel(final TournamentMatch match, final Locale locale) {
        if (match == null) {
            return null;
        }
        return messageSource.getMessage(
                "mail.tournament.field.match.value",
                new Object[] {match.getRoundNumber(), match.getMatchIndex() + 1},
                "Round " + match.getRoundNumber() + ", match " + (match.getMatchIndex() + 1),
                locale);
    }

    private String teamName(final TournamentTeam team, final Locale locale) {
        if (team == null) {
            return null;
        }
        final Long teamId = team.getId();
        if (teamId == null) {
            return null;
        }
        if (team.getName() != null
                && !team.getName().isBlank()
                && !isLegacyGeneratedSoloTeamName(team)) {
            return team.getName();
        }
        final Integer displayNumber =
                team.getSeedPosition() == null ? teamId.intValue() : team.getSeedPosition();
        return messageSource.getMessage(
                "tournament.team.solo.name",
                new Object[] {displayNumber},
                "Solo squad #" + displayNumber,
                locale);
    }

    private String message(final String code, final Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }

    private static Locale recipientLocale(final User user) {
        return user == null ? Locale.ENGLISH : UserLanguages.toLocale(user.getPreferredLanguage());
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
    }

    private static String displayName(final User user) {
        final String firstName = clean(user.getName());
        final String lastName = clean(user.getLastName());

        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        final String username = clean(user.getUsername());
        if (username != null) {
            return username;
        }

        return user.getEmail();
    }

    private static String clean(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isLegacyGeneratedSoloTeamName(final TournamentTeam team) {
        if (team.getOrigin() != TournamentTeamOrigin.SOLO_POOL || team.getName() == null) {
            return false;
        }
        final String normalized = team.getName().trim();
        return normalized.matches("(?i)Solo squad #\\d+")
                || normalized.matches("Equipo individual #\\d+");
    }

    private static String stripTrailingSlash(final String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
