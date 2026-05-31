package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Tournament;
import ar.edu.itba.paw.models.TournamentMatch;
import ar.edu.itba.paw.models.TournamentTeam;
import ar.edu.itba.paw.models.TournamentTeamMember;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.models.types.TournamentFormat;
import ar.edu.itba.paw.models.types.TournamentMatchStatus;
import ar.edu.itba.paw.models.types.TournamentStatus;
import ar.edu.itba.paw.models.types.TournamentTeamOrigin;
import ar.edu.itba.paw.persistence.TournamentTeamDao;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TournamentMailServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-04-05T00:00:00Z");

    @Mock private TournamentTeamDao tournamentTeamDao;

    private RecordingMailDispatchService mailDispatchService;
    private TournamentMailServiceImpl tournamentMailService;

    @BeforeEach
    public void setUp() {
        mailDispatchService = new RecordingMailDispatchService();
        tournamentMailService =
                new TournamentMailServiceImpl(tournamentTeamDao, mailDispatchService);
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
        // 2. Exercise
        tournamentMailService.sendBracketPublishedEmail(tournament);

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstUser.getEmail(), secondUser.getEmail()),
                mailDispatchService.recipients);
        Assertions.assertEquals(
                List.of("bracket-published", "bracket-published"), mailDispatchService.actions);
    }

    @Test
    public void sendMatchResultEmailSendsToParticipants() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.IN_PROGRESS);
        final User participant = UserUtils.getUser(2L);
        final TournamentTeam winner = team(20L, tournament, "Team A");
        final TournamentTeam loser = team(21L, tournament, "Team B");
        final TournamentMatch match = match(40L, tournament, winner, loser);
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, winner, participant)));

        // 2. Exercise
        tournamentMailService.sendMatchResultEmail(tournament, match, winner, loser);

        // 3. Assert
        Assertions.assertEquals(List.of("match-result"), mailDispatchService.actions);
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void sendCompletedEmailSendsChampionToParticipants() {
        // 1. Arrange
        final Tournament tournament = tournament(10L, TournamentStatus.COMPLETED);
        final User participant = UserUtils.getUser(2L);
        final TournamentTeam champion = team(20L, tournament, "Champions");
        Mockito.when(tournamentTeamDao.findMembersByTournament(10L))
                .thenReturn(List.of(member(30L, champion, participant)));

        // 2. Exercise
        tournamentMailService.sendTournamentCompletedEmail(tournament, champion);

        // 3. Assert
        Assertions.assertEquals(List.of("completed"), mailDispatchService.actions);
        Assertions.assertEquals(List.of(participant.getEmail()), mailDispatchService.recipients);
        Assertions.assertEquals(List.of(champion), mailDispatchService.teams);
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> recipients = new ArrayList<>();
        private final List<String> actions = new ArrayList<>();
        private final List<TournamentTeam> teams = new ArrayList<>();

        @Override
        public void sendTournamentBracketPublished(
                final User recipient, final Tournament tournament) {
            actions.add("bracket-published");
            recipients.add(recipient.getEmail());
        }

        @Override
        public void sendTournamentMatchResult(
                final User recipient,
                final Tournament tournament,
                final TournamentMatch match,
                final TournamentTeam winner,
                final TournamentTeam loser) {
            actions.add("match-result");
            recipients.add(recipient.getEmail());
        }

        @Override
        public void sendTournamentCompleted(
                final User recipient, final Tournament tournament, final TournamentTeam champion) {
            actions.add("completed");
            recipients.add(recipient.getEmail());
            teams.add(champion);
        }

        @Override
        public void sendTournamentCancelled(final User recipient, final Tournament tournament) {
            actions.add("cancelled");
            recipients.add(recipient.getEmail());
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
