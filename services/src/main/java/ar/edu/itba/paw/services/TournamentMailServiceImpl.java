package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.TournamentLifecycleMailTemplateData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class TournamentMailServiceImpl implements TournamentMailService {

    private final TournamentTeamDao tournamentTeamDao;
    private final MailDispatchService mailDispatchService;
    private final ThymeleafMailTemplateRenderer templateRenderer;
    private final MessageSource messageSource;

    public TournamentMailServiceImpl(
            final TournamentTeamDao tournamentTeamDao,
            final MailDispatchService mailDispatchService,
            final ThymeleafMailTemplateRenderer templateRenderer,
            final MessageSource messageSource) {
        this.tournamentTeamDao = tournamentTeamDao;
        this.mailDispatchService = mailDispatchService;
        this.templateRenderer = templateRenderer;
        this.messageSource = messageSource;
    }

    @Override
    public void sendBracketPublishedEmail(final Tournament tournament) {
        for (final User recipient : recipients(tournament)) {
            final TournamentLifecycleMailTemplateData templateData =
                    buildTemplateData(recipient, tournament, null, null, null, null);
            final MailContent content =
                    templateRenderer.renderTournamentBracketPublishedEmail(templateData);
            mailDispatchService.dispatch(recipient.getEmail(), content);
        }
    }

    @Override
    public void sendMatchResultEmail(
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser) {
        for (final User recipient : recipients(tournament)) {
            final TournamentLifecycleMailTemplateData templateData =
                    buildTemplateData(recipient, tournament, match, winner, loser, null);
            final MailContent content =
                    templateRenderer.renderTournamentMatchResultEmail(templateData);
            mailDispatchService.dispatch(recipient.getEmail(), content);
        }
    }

    @Override
    public void sendTournamentCompletedEmail(
            final Tournament tournament, final TournamentTeam champion) {
        for (final User recipient : recipients(tournament)) {
            final TournamentLifecycleMailTemplateData templateData =
                    buildTemplateData(recipient, tournament, null, null, null, champion);
            final MailContent content =
                    templateRenderer.renderTournamentCompletedEmail(templateData);
            mailDispatchService.dispatch(recipient.getEmail(), content);
        }
    }

    @Override
    public void sendTournamentCancelledEmail(final Tournament tournament) {
        for (final User recipient : recipients(tournament)) {
            final TournamentLifecycleMailTemplateData templateData =
                    buildTemplateData(recipient, tournament, null, null, null, null);
            final MailContent content =
                    templateRenderer.renderTournamentCancelledEmail(templateData);
            mailDispatchService.dispatch(recipient.getEmail(), content);
        }
    }

    private List<User> recipients(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return List.of();
        }

        final Map<String, User> recipientsByIdentity = new LinkedHashMap<>();
        for (final TournamentTeamMember member :
                tournamentTeamDao.findMembersByTournament(tournament.getId())) {
            final User user = member == null ? null : member.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            recipientsByIdentity.putIfAbsent(userIdentity(user), user);
        }
        return List.copyOf(recipientsByIdentity.values());
    }

    private TournamentLifecycleMailTemplateData buildTemplateData(
            final User recipient,
            final Tournament tournament,
            final TournamentMatch match,
            final TournamentTeam winner,
            final TournamentTeam loser,
            final TournamentTeam champion) {
        final Locale locale = UserLanguages.toLocale(recipient.getPreferredLanguage());
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
        final Map<Long, Integer> teamDisplayNumbers = teamDisplayNumbers(tournament);

        return new TournamentLifecycleMailTemplateData(
                recipient.getEmail(),
                tournament.getTitle(),
                sportLabel,
                statusLabel,
                matchLabel(match, locale),
                teamName(winner, locale, teamDisplayNumbers),
                teamName(loser, locale, teamDisplayNumbers),
                teamName(champion, locale, teamDisplayNumbers),
                tournament.getAddress(),
                tournament.getStartsAt(),
                locale);
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

    private String teamName(
            final TournamentTeam team,
            final Locale locale,
            final Map<Long, Integer> teamDisplayNumbers) {
        if (team == null) {
            return null;
        }
        if (team.getName() != null
                && !team.getName().isBlank()
                && !isLegacyGeneratedSoloTeamName(team)) {
            return team.getName();
        }
        if (team.getId() == null) {
            return null;
        }
        final Integer displayNumber = teamDisplayNumbers.get(team.getId());
        return messageSource.getMessage(
                "tournament.team.solo.name",
                new Object[] {displayNumber == null ? team.getId() : displayNumber},
                "Solo squad #" + (displayNumber == null ? team.getId() : displayNumber),
                locale);
    }

    private Map<Long, Integer> teamDisplayNumbers(final Tournament tournament) {
        if (tournament == null || tournament.getId() == null) {
            return Map.of();
        }
        final List<TournamentTeam> teams = tournamentTeamDao.findByTournament(tournament.getId());
        if (teams == null || teams.isEmpty()) {
            return Map.of();
        }
        final Map<Long, Integer> displayNumbers = new LinkedHashMap<>();
        for (int index = 0; index < teams.size(); index++) {
            final TournamentTeam team = teams.get(index);
            if (team != null && team.getId() != null) {
                displayNumbers.put(team.getId(), index + 1);
            }
        }
        return displayNumbers;
    }

    private static boolean isLegacyGeneratedSoloTeamName(final TournamentTeam team) {
        if (team.getOrigin() != TournamentTeamOrigin.SOLO_POOL || team.getName() == null) {
            return false;
        }
        final String normalized = team.getName().trim();
        return normalized.matches("(?i)Solo squad #\\d+")
                || normalized.matches("Equipo individual #\\d+");
    }

    private static String userIdentity(final User user) {
        if (user.getId() != null) {
            return "id:" + user.getId();
        }
        return "email:" + user.getEmail().trim().toLowerCase(Locale.ROOT);
    }
}
