package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import ar.edu.itba.paw.services.internal.MatchParticipantDataService;
import ar.edu.itba.paw.services.mail.MailDispatchService;
import ar.edu.itba.paw.services.utils.MatchUtils;
import ar.edu.itba.paw.services.utils.UserUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

@ExtendWith(MockitoExtension.class)
public class MatchNotificationServiceImplTest {

    @Mock private MatchParticipantDataService matchParticipantDataService;

    private RecordingMailDispatchService mailDispatchService;
    private MatchNotificationServiceImpl matchNotificationService;

    @BeforeEach
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        mailDispatchService = new RecordingMailDispatchService();
        matchNotificationService =
                new MatchNotificationServiceImpl(matchParticipantDataService, mailDispatchService);
    }

    @AfterEach
    public void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    public void testNotifyMatchUpdatedSendsOneMailPerParticipant() {
        final Match match = createMatch(40L, "Updated Match", EventStatus.OPEN);
        final List<User> participants = List.of(UserUtils.getUser(2L), UserUtils.getUser(3L));
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(40L))
                .thenReturn(participants);

        matchNotificationService.notifyMatchUpdated(match);

        Assertions.assertEquals(
                List.of("match-updated", "match-updated"), mailDispatchService.actions);
        Assertions.assertEquals(
                List.of(participants.get(0).getEmail(), participants.get(1).getEmail()),
                mailDispatchService.recipients);
    }

    @Test
    public void testNotifyMatchCancelledSendsOneMailPerParticipant() {
        final Match match = createMatch(41L, "Cancelled Match", EventStatus.CANCELLED);
        final List<User> participants = List.of(UserUtils.getUser(2L), UserUtils.getUser(3L));
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(41L))
                .thenReturn(participants);

        matchNotificationService.notifyMatchCancelled(match);

        Assertions.assertEquals(
                List.of("match-cancelled", "match-cancelled"), mailDispatchService.actions);
        Assertions.assertEquals(
                List.of(participants.get(0).getEmail(), participants.get(1).getEmail()),
                mailDispatchService.recipients);
    }

    @Test
    public void testNotifyMatchUpdatedDoesNothingWhenNoParticipants() {
        final Match match = createMatch(42L, "Quiet Match", EventStatus.OPEN);
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(42L))
                .thenReturn(List.of());

        matchNotificationService.notifyMatchUpdated(match);

        Assertions.assertTrue(mailDispatchService.recipients.isEmpty());
        Assertions.assertTrue(mailDispatchService.actions.isEmpty());
    }

    @Test
    public void testNotifyRecurringMatchesUpdatedDeduplicatesAffectedParticipants() {
        // 1. Arrange
        final Match firstOccurrence =
                createRecurringMatch(50L, "Weekly Padel", EventStatus.OPEN, 600L, 1);
        final Match secondOccurrence =
                createRecurringMatch(51L, "Weekly Padel", EventStatus.OPEN, 600L, 2);
        final User firstParticipant = UserUtils.getUser(2L);
        final User secondParticipant = UserUtils.getUser(3L);
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(50L))
                .thenReturn(List.of(firstParticipant));
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(51L))
                .thenReturn(List.of(firstParticipant, secondParticipant));

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesUpdated(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstParticipant.getEmail(), secondParticipant.getEmail()),
                mailDispatchService.recipients);
        Assertions.assertEquals(List.of(2, 1), mailDispatchService.occurrenceCounts);
    }

    @Test
    public void testNotifyRecurringMatchesCancelledUsesPerRecipientAffectedDateCount() {
        // 1. Arrange
        final Match firstOccurrence =
                createRecurringMatch(52L, "Weekly Padel", EventStatus.CANCELLED, 600L, 1);
        final Match secondOccurrence =
                createRecurringMatch(53L, "Weekly Padel", EventStatus.CANCELLED, 600L, 2);
        final User firstParticipant = UserUtils.getUser(2L);
        final User secondParticipant = UserUtils.getUser(3L);
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(52L))
                .thenReturn(List.of(firstParticipant));
        Mockito.when(matchParticipantDataService.findConfirmedParticipants(53L))
                .thenReturn(List.of(firstParticipant, secondParticipant));

        // 2. Exercise
        matchNotificationService.notifyRecurringMatchesCancelled(
                List.of(firstOccurrence, secondOccurrence));

        // 3. Assert
        Assertions.assertEquals(
                List.of(firstParticipant.getEmail(), secondParticipant.getEmail()),
                mailDispatchService.recipients);
        Assertions.assertEquals(List.of(2, 1), mailDispatchService.occurrenceCounts);
    }

    @Test
    public void testNotifyHostPlayerLeftSendsMailToHost() {
        final Match match = createMatch(60L, "Weekly Padel", EventStatus.OPEN);
        final User player = UserUtils.getUser(2L);
        final User host = UserUtils.getUser(1L);

        matchNotificationService.notifyHostPlayerLeft(match, player);

        Assertions.assertEquals(List.of("player-left"), mailDispatchService.actions);
        Assertions.assertEquals(List.of(host.getEmail()), mailDispatchService.recipients);
    }

    @Test
    public void testNotifyPlayerRemovedByHostSendsMailToPlayer() {
        final Match match = createMatch(61L, "Weekly Padel", EventStatus.OPEN);
        final User player = UserUtils.getUser(2L);

        matchNotificationService.notifyPlayerRemovedByHost(match, player);

        Assertions.assertEquals(List.of("player-removed"), mailDispatchService.actions);
        Assertions.assertEquals(List.of(player.getEmail()), mailDispatchService.recipients);
    }

    private static class RecordingMailDispatchService implements MailDispatchService {

        private final List<String> recipients = new ArrayList<>();
        private final List<String> actions = new ArrayList<>();
        private final List<Integer> occurrenceCounts = new ArrayList<>();

        @Override
        public void sendMatchUpdated(final User recipient, final Match match) {
            actions.add("match-updated");
            recipients.add(recipient.getEmail());
        }

        @Override
        public void sendMatchCancelled(final User recipient, final Match match) {
            actions.add("match-cancelled");
            recipients.add(recipient.getEmail());
        }

        @Override
        public void sendRecurringMatchesUpdated(
                final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {
            actions.add("recurring-updated");
            recipients.add(recipient.getEmail());
            occurrenceCounts.add(occurrenceCount);
        }

        @Override
        public void sendRecurringMatchesCancelled(
                final User recipient, final Match firstAffectedMatch, final int occurrenceCount) {
            actions.add("recurring-cancelled");
            recipients.add(recipient.getEmail());
            occurrenceCounts.add(occurrenceCount);
        }

        @Override
        public void sendPlayerLeft(final User host, final Match match, final User player) {
            actions.add("player-left");
            recipients.add(host.getEmail());
        }

        @Override
        public void sendPlayerRemoved(final User player, final Match match) {
            actions.add("player-removed");
            recipients.add(player.getEmail());
        }
    }

    private static Match createMatch(final long id, final String title, final EventStatus status) {
        Match match =
                MatchUtils.createMatchWithId(
                        id, 1L, Sport.PADEL, Instant.parse("2026-04-06T18:00:00Z"), 10);
        match.setStatus(status);
        return match;
    }

    private static Match createRecurringMatch(
            final long id,
            final String title,
            final EventStatus status,
            final long seriesId,
            final int occurrenceIndex) {
        return new Match(
                id,
                Sport.PADEL,
                UserUtils.getUser(1L),
                "Downtown Club",
                null,
                null,
                title,
                "Description",
                Instant.parse("2026-04-06T18:00:00Z").plusSeconds(604800L * occurrenceIndex),
                Instant.parse("2026-04-06T19:00:00Z").plusSeconds(604800L * occurrenceIndex),
                10,
                BigDecimal.ZERO,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                status,
                0,
                null,
                MatchUtils.getMatchSeries(seriesId, UserUtils.getUser(1L)),
                occurrenceIndex,
                false,
                null,
                null,
                null);
    }
}
