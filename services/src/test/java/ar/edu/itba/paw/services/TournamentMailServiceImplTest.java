package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.UserLanguages;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.mail.MailContent;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.mail.ThymeleafMailTemplateRenderer;
import ar.edu.itba.paw.services.mail.TournamentLifecycleMailTemplateData;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
public class TournamentMailServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentTeamDao tournamentTeamDao;
    @Mock private ThymeleafMailTemplateRenderer templateRenderer;
    @Mock private MessageSource messageSource;

    private RecordingMailDispatchService mailDispatchService;
    private TournamentMailServiceImpl tournamentMailService;

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        tournamentMailService =
                new TournamentMailServiceImpl(
                        tournamentTeamDao, mailDispatchService, templateRenderer, messageSource);
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.eq("mail.tournament.field.match.value"),
                                ArgumentMatchers.any(Object[].class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        Mockito.lenient()
                .when(
                        messageSource.getMessage(
                                ArgumentMatchers.eq("tournament.team.solo.name"),
                                ArgumentMatchers.any(Object[].class),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(
                        invocation -> {
                            final Object[] args = invocation.getArgument(1);
                            final Locale locale = invocation.getArgument(3);
                            return ("es".equals(locale.getLanguage())
                                            ? "Equipo individual #"
                                            : "Solo squad #")
                                    + args[0];
                        });
        Mockito.lenient()
                .when(tournamentTeamDao.findByTournament(ArgumentMatchers.anyLong()))
                .thenReturn(List.of());
    }

    @Test
    public void sendBracketPublishedEmailDeduplicatesRecipients() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.IN_PROGRESS);
        final User firstUser = UserUtils.getUser(2L);
        final User secondUser = UserUtils.getUser(3L);
        final TournamentTeam firstTeam = team(20L, tournament, "Team A");
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(
                        List.of(
                                member(30L, firstTeam, firstUser),
                                member(31L, firstTeam, firstUser),
                                member(32L, team(21L, tournament, "Team B"), secondUser)));
        Mockito.when(templateRenderer.renderTournamentBracketPublishedEmail(ArgumentMatchers.any()))
                .thenReturn(new MailContent("published", "<p>published</p>", "published"));

        // 2. Exercise
        tournamentMailService.sendBracketPublishedEmail(tournament);

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstUser.getEmail(), secondUser.getEmail()),
                mailDispatchService.recipients);
        Assertions.assertEquals(2, mailDispatchService.contents.size());
    }

    @Test
    public void sendMatchResultEmailUsesRecipientPreferredLanguage() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.IN_PROGRESS);
        final User participant =
                new User(
                        2L,
                        "player@test.com",
                        "player",
                        "Jamie",
                        "Rivera",
                        null,
                        null,
                        UserLanguages.SPANISH);
        final TournamentTeam winner = team(20L, tournament, "Team A");
        final TournamentTeam loser = team(21L, tournament, "Team B");
        final TournamentMatch match = match(40L, tournament, winner, loser);
        final AtomicReference<Locale> capturedLocale = new AtomicReference<>();
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, winner, participant)));
        Mockito.when(
                        messageSource.getMessage(
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.isNull(),
                                ArgumentMatchers.anyString(),
                                ArgumentMatchers.any(Locale.class)))
                .thenAnswer(
                        invocation -> {
                            capturedLocale.set(invocation.getArgument(3));
                            return invocation.getArgument(2);
                        });
        Mockito.when(templateRenderer.renderTournamentMatchResultEmail(ArgumentMatchers.any()))
                .thenReturn(new MailContent("result", "<p>result</p>", "result"));

        // 2. Exercise
        tournamentMailService.sendMatchResultEmail(tournament, match, winner, loser);

        // 3. Assert
        Assertions.assertEquals(Locale.of("es"), capturedLocale.get());
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void sendCompletedEmailPassesChampionToTemplateData() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.COMPLETED);
        final User participant = UserUtils.getUser(2L);
        final TournamentTeam champion = team(20L, tournament, "Champions");
        final AtomicReference<TournamentLifecycleMailTemplateData> capturedData =
                new AtomicReference<>();
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, champion, participant)));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(List.of(champion));
        Mockito.when(templateRenderer.renderTournamentCompletedEmail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedData.set(invocation.getArgument(0));
                            return new MailContent("completed", "<p>completed</p>", "completed");
                        });

        // 2. Exercise
        tournamentMailService.sendTournamentCompletedEmail(tournament, champion);

        // 3. Assert
        Assertions.assertEquals("Champions", capturedData.get().getChampionName());
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void sendCompletedEmailLocalizesUnnamedChampion() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.COMPLETED);
        final User participant =
                new User(
                        2L,
                        "player@test.com",
                        "player",
                        "Jamie",
                        "Rivera",
                        null,
                        null,
                        UserLanguages.SPANISH);
        final TournamentTeam champion = team(20L, tournament, null);
        final AtomicReference<TournamentLifecycleMailTemplateData> capturedData =
                new AtomicReference<>();
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, champion, participant)));
        Mockito.when(tournamentTeamDao.findByTournament(10L)).thenReturn(List.of(champion));
        Mockito.when(templateRenderer.renderTournamentCompletedEmail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedData.set(invocation.getArgument(0));
                            return new MailContent("completed", "<p>completed</p>", "completed");
                        });

        // 2. Exercise
        tournamentMailService.sendTournamentCompletedEmail(tournament, champion);

        // 3. Assert
        Assertions.assertEquals("Equipo individual #1", capturedData.get().getChampionName());
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void sendCompletedEmailRelocalizesLegacyPlaceholderChampion() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.COMPLETED);
        final User participant = UserUtils.getUser(2L);
        final TournamentTeam earlierTeam = team(19L, tournament, null);
        final TournamentTeam champion = team(20L, tournament, "Equipo individual #1");
        final AtomicReference<TournamentLifecycleMailTemplateData> capturedData =
                new AtomicReference<>();
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, champion, participant)));
        Mockito.when(tournamentTeamDao.findByTournament(10L))
                .thenReturn(List.of(earlierTeam, champion));
        Mockito.when(templateRenderer.renderTournamentCompletedEmail(ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            capturedData.set(invocation.getArgument(0));
                            return new MailContent("completed", "<p>completed</p>", "completed");
                        });

        // 2. Exercise
        tournamentMailService.sendTournamentCompletedEmail(tournament, champion);

        // 3. Assert
        Assertions.assertEquals("Solo squad #2", capturedData.get().getChampionName());
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> recipients = new ArrayList<>();
        private final List<MailContent> contents = new ArrayList<>();

        @Override
        public void dispatch(final String recipientEmail, final MailContent content) {
            recipients.add(recipientEmail);
            contents.add(content);
        }
    }

    private static Tournament tournament(final long id, final TournamentStatus status) {
        return new Tournament(
                id,
                UserUtils.getUser(1L),
                Sport.FOOTBALL,
                "Saturday Cup",
                "Friendly tournament",
                "Club Street 123",
                -34.60,
                -58.38,
                NOW.plusSeconds(86400),
                NOW.plusSeconds(90000),
                BigDecimal.ZERO,
                null,
                TournamentFormat.SINGLE_ELIMINATION,
                4,
                1,
                true,
                false,
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                status,
                NOW,
                NOW);
    }

    private static TournamentTeam team(
            final long id, final Tournament tournament, final String name) {
        return new TournamentTeam(id, tournament, name, TournamentTeamOrigin.SOLO_POOL, null, NOW);
    }

    private static TournamentTeamMember member(
            final long id, final TournamentTeam team, final User user) {
        return new TournamentTeamMember(id, team, user, false, NOW);
    }

    private static TournamentMatch match(
            final long id,
            final Tournament tournament,
            final TournamentTeam teamA,
            final TournamentTeam teamB) {
        return new TournamentMatch(
                id,
                tournament,
                1,
                0,
                teamA,
                teamB,
                null,
                null,
                null,
                null,
                null,
                null,
                TournamentMatchStatus.DONE,
                null,
                null,
                NOW,
                NOW);
    }
}
