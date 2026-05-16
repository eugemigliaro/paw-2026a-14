package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.MatchSeries;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.query.EventTimeFilter;
import ar.edu.itba.paw.models.query.MatchSort;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.models.types.Sport;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class MatchJpaDaoTest {

    @Autowired private MatchDao matchDao;
    @PersistenceContext private EntityManager em;

    private User host;

    @BeforeEach
    public void setUp() {
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, created_at, updated_at)"
                                + " VALUES (1, 'host', 'host@test.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .executeUpdate();
        em.flush();
        em.clear();

        host = em.find(User.class, 1L);
    }

    @Test
    public void shouldPersistMatchInDatabaseWhenCreatingMatch() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);

        final Match created =
                matchDao.createMatch(
                        host,
                        "Stadium A",
                        "Tennis Singles",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        em.flush();
        em.clear();
        final Match persisted = em.find(Match.class, created.getId());

        Assertions.assertNotNull(created.getId());
        Assertions.assertEquals(Sport.TENNIS, created.getSport());
        Assertions.assertEquals(host.getId(), persisted.getHost().getId());
        Assertions.assertEquals("Tennis Singles", persisted.getTitle());
        Assertions.assertEquals(Sport.TENNIS, persisted.getSport());
        Assertions.assertEquals(EventVisibility.PUBLIC, persisted.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.DIRECT, persisted.getJoinPolicy());
        Assertions.assertEquals(EventStatus.OPEN, persisted.getStatus());
        Assertions.assertEquals(4, persisted.getMaxPlayers());
        Assertions.assertEquals(1, countMatches());
    }

    @Test
    public void shouldPersistSeriesAndOccurrencesWhenCreatingRecurringMatch() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);

        final Long seriesId =
                matchDao.createMatchSeries(
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);

        final MatchSeries matchSeries =
                new MatchSeries(
                        seriesId,
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2,
                        null,
                        null);

        final Match first =
                matchDao.createMatch(
                        host,
                        "Stadium A",
                        "Weekly Tennis",
                        "Open match",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        matchSeries,
                        1);
        final Match second =
                matchDao.createMatch(
                        host,
                        "Stadium A",
                        "Weekly Tennis",
                        "Open match",
                        startsAt.plusWeeks(1).toInstant(),
                        endsAt.plusWeeks(1).toInstant(),
                        4,
                        BigDecimal.ZERO,
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        matchSeries,
                        2);

        em.flush();
        em.clear();

        final int seriesCount =
                ((Number)
                                em.createNativeQuery(
                                                "SELECT COUNT(*) FROM match_series WHERE id = :seriesId"
                                                        + " AND host_user_id = :hostUserId AND frequency = 'weekly'")
                                        .setParameter("seriesId", seriesId)
                                        .setParameter("hostUserId", host.getId())
                                        .getSingleResult())
                        .intValue();
        final int occurrencesCount =
                ((Number)
                                em.createNativeQuery(
                                                "SELECT COUNT(*) FROM matches WHERE series_id = :seriesId")
                                        .setParameter("seriesId", seriesId)
                                        .getSingleResult())
                        .intValue();
        final int secondOccurrenceIndex =
                ((Number)
                                em.createNativeQuery(
                                                "SELECT series_occurrence_index FROM matches WHERE id = :matchId")
                                        .setParameter("matchId", second.getId())
                                        .getSingleResult())
                        .intValue();
        final List<Match> occurrences = matchDao.findSeriesOccurrences(seriesId);

        Assertions.assertEquals(1, seriesCount);
        Assertions.assertEquals(2, occurrencesCount);
        Assertions.assertEquals(2, secondOccurrenceIndex);
        Assertions.assertEquals(2, occurrences.size());
        Assertions.assertEquals(first.getId(), occurrences.get(0).getId());
        Assertions.assertEquals(second.getId(), occurrences.get(1).getId());
        Assertions.assertEquals(seriesId, occurrences.get(0).getSeries().getId());
        Assertions.assertEquals(1, occurrences.get(0).getSeriesOccurrenceIndex());
    }

    @Test
    public void shouldCreateRecurringOccurrenceWithNullSeriesOccurrenceIndex() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);
        final Long seriesId =
                matchDao.createMatchSeries(
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);

        final MatchSeries matchSeries =
                new MatchSeries(
                        seriesId,
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2,
                        null,
                        null);

        Assertions.assertThrows(
                PersistenceException.class,
                () -> {
                    matchDao.createMatch(
                            host,
                            "Stadium A",
                            "Incomplete Weekly Tennis",
                            "Open match",
                            startsAt.toInstant(),
                            endsAt.toInstant(),
                            4,
                            BigDecimal.ZERO,
                            Sport.TENNIS,
                            EventVisibility.PUBLIC,
                            EventJoinPolicy.DIRECT,
                            EventStatus.OPEN,
                            null,
                            matchSeries,
                            null);
                    em.flush();
                });
    }

    @Test
    public void testFindPublicEventsBySearchText() {
        final User otherHost = createUser("other-host", "other-host@example.com");

        matchDao.createMatch(
                otherHost,
                "River Court",
                "Morning Football",
                "Fast 5v5 match",
                ZonedDateTime.now().plusDays(1).toInstant(),
                null,
                10,
                BigDecimal.ZERO,
                Sport.FOOTBALL,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                null);
        createOpenPublicMatch(
                "Basketball Session",
                "Stretching",
                Sport.BASKETBALL,
                10,
                ZonedDateTime.now().plusDays(1));
        em.flush();
        em.clear();

        for (final String query : List.of("football", "fast", "river", otherHost.getUsername())) {
            final List<Match> result = findPublicMatchesByQuery(query);

            Assertions.assertEquals(1, result.size(), query);
            Assertions.assertEquals("Morning Football", result.get(0).getTitle(), query);
        }
    }

    @Test
    public void testFindPublicEventsIncludesApprovalRequiredVisibility() {
        matchDao.createMatch(
                host,
                "Test Address",
                "Approval Required Match",
                "Description",
                ZonedDateTime.now().plusDays(1).toInstant(),
                null,
                10,
                BigDecimal.ZERO,
                Sport.FOOTBALL,
                EventVisibility.PUBLIC,
                EventJoinPolicy.APPROVAL_REQUIRED,
                EventStatus.OPEN,
                null);
        createMatchWithPolicy(
                "Legacy Invite Only Match",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                host,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);
        createMatchWithPolicy(
                "Private Match",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                host,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertTrue(
                result.stream().anyMatch(m -> "Approval Required Match".equals(m.getTitle())));
        Assertions.assertTrue(
                result.stream().noneMatch(m -> "Legacy Invite Only Match".equals(m.getTitle())));
        Assertions.assertTrue(result.stream().noneMatch(m -> "Private Match".equals(m.getTitle())));
    }

    @Test
    public void testFindPublicEventsBySportFilter() {
        createOpenPublicMatch(
                "Morning Football",
                "Fast 5v5 match",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusDays(1));
        createOpenPublicMatch(
                "Evening Padel", "Doubles games", Sport.PADEL, 10, ZonedDateTime.now().plusDays(1));
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Sport.PADEL, result.get(0).getSport());
    }

    @Test
    public void testFindPublicEventsExcludesFullEvents() {
        final Match fullMatch =
                createOpenPublicMatch(
                        "Full Match",
                        "No spots left",
                        Sport.FOOTBALL,
                        2,
                        ZonedDateTime.now().plusDays(1));
        createOpenPublicMatch(
                "Open Match", "Has spots", Sport.FOOTBALL, 10, ZonedDateTime.now().plusDays(1));
        joinPlayers(fullMatch.getId(), 2);
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Open Match", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicEventsWithPagination() {
        createOpenPublicMatch(
                "Event 1", "Description", Sport.FOOTBALL, 10, ZonedDateTime.now().plusHours(2));
        createOpenPublicMatch(
                "Event 2", "Description", Sport.FOOTBALL, 10, ZonedDateTime.now().plusHours(3));
        createOpenPublicMatch(
                "Event 3", "Description", Sport.FOOTBALL, 10, ZonedDateTime.now().plusHours(4));
        em.flush();
        em.clear();

        final List<Match> firstPage =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        2);
        final List<Match> secondPage =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        2,
                        2);

        Assertions.assertEquals(2, firstPage.size());
        Assertions.assertEquals(1, secondPage.size());
        Assertions.assertEquals(
                3,
                matchDao.countPublicMatches(
                        null, List.of(), EventTimeFilter.WEEK, null, null, ZoneId.systemDefault()));
    }

    @Test
    public void testFindPublicEventsWithAllTimeFilterShowsOnlyUpcoming() {
        createOpenPublicMatch(
                "Past Match",
                "Already happened",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().minusHours(2));
        createOpenPublicMatch(
                "Upcoming Match",
                "Will happen soon",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusHours(2));
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.ALL,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Upcoming Match", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicEventsByMultipleSportFilters() {
        createOpenPublicMatch(
                "Morning Football",
                "Fast 5v5 match",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusDays(1));
        createOpenPublicMatch(
                "Evening Padel", "Doubles games", Sport.PADEL, 10, ZonedDateTime.now().plusDays(1));
        createOpenPublicMatch(
                "Basketball Session",
                "Stretching",
                Sport.BASKETBALL,
                10,
                ZonedDateTime.now().plusDays(1));
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.FOOTBALL, Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(
                result.stream()
                        .allMatch(
                                m ->
                                        m.getSport() == Sport.FOOTBALL
                                                || m.getSport() == Sport.PADEL));
    }

    @Test
    public void testFindPublicEventsByPriceRange() {
        createOpenPublicMatchWithPrice(
                "Budget Match",
                "Free friendly session",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusDays(1),
                BigDecimal.ZERO);
        createOpenPublicMatchWithPrice(
                "Premium Match",
                "Club booking included",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusDays(1),
                new BigDecimal("25"));
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Premium Match", result.get(0).getTitle());
        Assertions.assertEquals(
                1,
                matchDao.countPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindPublicEventsExcludesDeletedMatches() {
        createOpenPublicMatch(
                "Active Match",
                "Fast 5v5 match",
                Sport.FOOTBALL,
                10,
                ZonedDateTime.now().plusDays(1));
        final Match toDelete =
                matchDao.createMatch(
                        host,
                        "Deleted Address",
                        "Deleted Match",
                        "Deleted Description",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        em.flush();
        em.clear();

        matchDao.softDeleteMatch(toDelete.getId(), host, "Violation");
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Active Match", result.get(0).getTitle());
    }

    @Test
    public void testFindPublicMatchesSortsByDistanceAndPlacesNullCoordinatesLast() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final Match far =
                matchDao.createMatch(
                        host,
                        "Far Address",
                        "Far Match",
                        "Open match",
                        startsAt.plusHours(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.8,
                        -58.8);
        final Match nearby =
                matchDao.createMatch(
                        host,
                        "Near Address",
                        "Near Match",
                        "Open match",
                        startsAt.plusHours(2).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.61,
                        -58.39);
        final Match addressOnly =
                matchDao.createMatch(
                        host,
                        "Address Only",
                        "Address Only Match",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        (Double) null,
                        (Double) null);
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.PADEL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.DISTANCE,
                        ZoneId.systemDefault(),
                        -34.6,
                        -58.4,
                        0,
                        20);

        Assertions.assertTrue(result.size() >= 3);
        Assertions.assertEquals(nearby.getId(), result.get(0).getId());
        Assertions.assertEquals(far.getId(), result.get(1).getId());
        Assertions.assertEquals(addressOnly.getId(), result.get(2).getId());
    }

    @Test
    public void testFindMatchByIdIncludesAvailability() {
        final Match created =
                matchDao.createMatch(
                        host,
                        "Downtown Club",
                        "Padel Morning",
                        "Friendly doubles session",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        em.flush();
        em.clear();

        final Match found = matchDao.findMatchById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals("Padel Morning", found.getTitle());
        Assertions.assertEquals(4, found.getAvailableSpots());
    }

    @Test
    public void testFindMatchByIdIncludesJoinedPlayersForNonPublicMatch() {
        final Match created =
                matchDao.createMatch(
                        host,
                        "Downtown Club",
                        "Private Football Night",
                        "Private 5v5",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        2,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        null);
        joinPlayers(created.getId(), 1);
        em.flush();
        em.clear();

        final Match found = matchDao.findMatchById(created.getId()).orElseThrow();

        Assertions.assertEquals(created.getId(), found.getId());
        Assertions.assertEquals(EventVisibility.PRIVATE, found.getVisibility());
        Assertions.assertEquals(1, found.getJoinedPlayers());
    }

    @Test
    public void shouldFindPublicMatchByIdOnlyWhenMatchIsPublicAndOpen() {
        final Match publicOpen =
                matchDao.createMatch(
                        host,
                        "Public Address",
                        "Public Open Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        final Match privateOpen =
                matchDao.createMatch(
                        host,
                        "Private Address",
                        "Private Open Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.OPEN,
                        null);
        final Match publicCancelled =
                matchDao.createMatch(
                        host,
                        "Cancelled Address",
                        "Public Cancelled Match",
                        "Cancelled match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        8,
                        BigDecimal.ZERO,
                        Sport.FOOTBALL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.CANCELLED,
                        null);
        em.flush();
        em.clear();

        Assertions.assertTrue(matchDao.findPublicMatchById(publicOpen.getId()).isPresent());
        Assertions.assertFalse(matchDao.findPublicMatchById(privateOpen.getId()).isPresent());
        Assertions.assertFalse(matchDao.findPublicMatchById(publicCancelled.getId()).isPresent());
        Assertions.assertFalse(matchDao.findPublicMatchById(999_998L).isPresent());
    }

    @Test
    public void shouldUpdateMatchAndPersistNewValuesInDatabase() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        em.flush();
        em.clear();

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        host,
                        "Updated Stadium",
                        "Updated Title",
                        "Updated description",
                        ZonedDateTime.now().plusDays(2).toInstant(),
                        null,
                        10,
                        new BigDecimal("25.50"),
                        Sport.PADEL,
                        EventVisibility.PRIVATE,
                        EventJoinPolicy.INVITE_ONLY,
                        EventStatus.DRAFT,
                        null,
                        -34.61,
                        -58.38);

        em.flush();
        em.clear();
        final Match updatedMatch = em.find(Match.class, created.getId());

        Assertions.assertTrue(updated);
        Assertions.assertEquals("Updated Stadium", updatedMatch.getAddress());
        Assertions.assertEquals("Updated Title", updatedMatch.getTitle());
        Assertions.assertEquals("Updated description", updatedMatch.getDescription());
        Assertions.assertEquals(Sport.PADEL, updatedMatch.getSport());
        Assertions.assertEquals(EventVisibility.PRIVATE, updatedMatch.getVisibility());
        Assertions.assertEquals(EventJoinPolicy.INVITE_ONLY, updatedMatch.getJoinPolicy());
        Assertions.assertEquals(EventStatus.DRAFT, updatedMatch.getStatus());
        Assertions.assertEquals(10, updatedMatch.getMaxPlayers());
        Assertions.assertEquals(
                0, new BigDecimal("25.50").compareTo(updatedMatch.getPricePerPlayer()));
        Assertions.assertEquals(-34.61, updatedMatch.getLatitude());
        Assertions.assertEquals(-58.38, updatedMatch.getLongitude());
    }

    @Test
    public void testUpdateMatchRejectsWrongHostUserId() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        final User other = createUser("other-host", "other-host@test.com");
        em.flush();
        em.clear();

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        other,
                        "Updated Address",
                        "Updated Title",
                        "Updated Description",
                        ZonedDateTime.now().plusDays(2).toInstant(),
                        ZonedDateTime.now().plusDays(2).plusHours(2).toInstant(),
                        10,
                        new BigDecimal("15"),
                        Sport.TENNIS,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);

        em.flush();
        em.clear();
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(updated);
        Assertions.assertEquals("Test Address", found.getAddress());
        Assertions.assertEquals("Original Title", found.getTitle());
        Assertions.assertEquals(Sport.FOOTBALL, found.getSport());
        Assertions.assertEquals(8, found.getMaxPlayers());
    }

    @Test
    public void testCreateFindAndUpdateMatchRoundTripsCoordinates() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final Match created =
                matchDao.createMatch(
                        host,
                        "Court Address",
                        "Pinned Match",
                        "Open match",
                        startsAt.toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.61,
                        -58.38);
        em.flush();
        em.clear();

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertEquals(-34.61, found.getLatitude());
        Assertions.assertEquals(-58.38, found.getLongitude());

        final boolean updated =
                matchDao.updateMatch(
                        created.getId(),
                        host,
                        "New Court Address",
                        "Moved Match",
                        "Open match",
                        startsAt.plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        -34.5,
                        -58.5);

        em.flush();
        em.clear();
        final Match moved = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(updated);
        Assertions.assertEquals(-34.5, moved.getLatitude());
        Assertions.assertEquals(-58.5, moved.getLongitude());
    }

    @Test
    public void testCreateMatchAllowsAddressOnlyCoordinates() {
        final Match created =
                matchDao.createMatch(
                        host,
                        "Address Only",
                        "Address Only Match",
                        "Open match",
                        ZonedDateTime.now().plusDays(1).toInstant(),
                        null,
                        4,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null);
        em.flush();
        em.clear();

        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertNull(found.getLatitude());
        Assertions.assertNull(found.getLongitude());
    }

    @Test
    public void testMatchCoordinateConstraintsRejectInvalidRows() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);

        Assertions.assertThrows(
                PersistenceException.class,
                () -> {
                    matchDao.createMatch(
                            host,
                            "Invalid Address",
                            "Missing Pair",
                            "Open match",
                            startsAt.toInstant(),
                            null,
                            4,
                            BigDecimal.ZERO,
                            Sport.PADEL,
                            EventVisibility.PUBLIC,
                            EventJoinPolicy.DIRECT,
                            EventStatus.OPEN,
                            null,
                            -34.61,
                            null);
                    em.flush();
                });

        Assertions.assertThrows(
                PersistenceException.class,
                () -> {
                    matchDao.createMatch(
                            host,
                            "Invalid Address",
                            "Out of Range",
                            "Open match",
                            startsAt.toInstant(),
                            null,
                            4,
                            BigDecimal.ZERO,
                            Sport.PADEL,
                            EventVisibility.PUBLIC,
                            EventJoinPolicy.DIRECT,
                            EventStatus.OPEN,
                            null,
                            -91.0,
                            -58.38);
                    em.flush();
                });
    }

    @Test
    public void shouldCancelMatchAndPersistStatusInDatabase() {
        final Match created = createOpenMatch("Cancellable", ZonedDateTime.now().plusDays(1), 8);
        em.flush();
        em.clear();

        final boolean cancelled = matchDao.cancelMatch(created.getId(), host);

        em.flush();
        em.clear();
        final String status =
                (String)
                        em.createNativeQuery("SELECT status FROM matches WHERE id = :matchId")
                                .setParameter("matchId", created.getId())
                                .getSingleResult();
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(cancelled);
        Assertions.assertEquals("cancelled", status);
        Assertions.assertEquals(EventStatus.CANCELLED, found.getStatus());
        Assertions.assertEquals(1, countMatches());
    }

    @Test
    public void testCancelMatchCancelsOnlySelectedRecurringOccurrence() {
        final ZonedDateTime startsAt = ZonedDateTime.now().plusDays(1);
        final ZonedDateTime endsAt = startsAt.plusMinutes(90);
        final Long seriesId =
                matchDao.createMatchSeries(
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2);

        final MatchSeries matchSeries =
                new MatchSeries(
                        seriesId,
                        host,
                        "weekly",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        ZoneId.systemDefault().getId(),
                        null,
                        2,
                        null,
                        null);

        final Match firstOccurrence =
                matchDao.createMatch(
                        host,
                        "Original Address",
                        "Weekly Padel",
                        "First occurrence",
                        startsAt.toInstant(),
                        endsAt.toInstant(),
                        8,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        matchSeries,
                        1);
        final Match secondOccurrence =
                matchDao.createMatch(
                        host,
                        "Original Address",
                        "Weekly Padel",
                        "Second occurrence",
                        startsAt.plusWeeks(1).toInstant(),
                        endsAt.plusWeeks(1).toInstant(),
                        8,
                        BigDecimal.ZERO,
                        Sport.PADEL,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        EventStatus.OPEN,
                        null,
                        matchSeries,
                        2);
        em.flush();
        em.clear();

        final boolean cancelled = matchDao.cancelMatch(secondOccurrence.getId(), host);

        em.flush();
        em.clear();
        final List<Match> occurrences = matchDao.findSeriesOccurrences(seriesId);

        Assertions.assertTrue(cancelled);
        Assertions.assertEquals(firstOccurrence.getId(), occurrences.get(0).getId());
        Assertions.assertEquals(EventStatus.OPEN, occurrences.get(0).getStatus());
        Assertions.assertEquals(secondOccurrence.getId(), occurrences.get(1).getId());
        Assertions.assertEquals(EventStatus.CANCELLED, occurrences.get(1).getStatus());
    }

    @Test
    public void testCancelMatchRejectsWrongHostUserId() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        final User other = createUser("other-host", "other-host@test.com");
        em.flush();
        em.clear();

        final boolean cancelled = matchDao.cancelMatch(created.getId(), other);

        em.flush();
        em.clear();
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertFalse(cancelled);
        Assertions.assertEquals(EventStatus.OPEN, found.getStatus());
    }

    @Test
    public void testCancelMatchUpdatesTimestamp() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        em.flush();

        em.createNativeQuery(
                        "UPDATE matches SET updated_at = TIMESTAMPADD(SECOND, -5, updated_at) WHERE id = :matchId")
                .setParameter("matchId", created.getId())
                .executeUpdate();
        em.flush();

        final Instant beforeCancel =
                em.createQuery(
                                "SELECT m.updatedAt FROM Match m WHERE m.id = :matchId",
                                Instant.class)
                        .setParameter("matchId", created.getId())
                        .getSingleResult();

        final boolean cancelled = matchDao.cancelMatch(created.getId(), host);

        em.flush();

        final Instant afterUpdate =
                em.createQuery(
                                "SELECT m.updatedAt FROM Match m WHERE m.id = :matchId",
                                Instant.class)
                        .setParameter("matchId", created.getId())
                        .getSingleResult();

        Assertions.assertTrue(cancelled);
        Assertions.assertNotNull(beforeCancel);
        Assertions.assertNotNull(afterUpdate);
        Assertions.assertTrue(afterUpdate.isAfter(beforeCancel));
    }

    @Test
    public void shouldSoftDeleteMatchAndPersistAuditFields() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        final User admin = createUser("admin", "admin@test.com");
        em.flush();
        em.clear();

        final boolean deleted = matchDao.softDeleteMatch(created.getId(), admin, "Violation");

        em.flush();
        em.clear();
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(deleted);
        Assertions.assertTrue(found.isDeleted());
        Assertions.assertEquals(admin.getId(), found.getDeletedByUser().getId());
        Assertions.assertEquals("Violation", found.getDeleteReason());
        Assertions.assertNotNull(found.getDeletedAt());
        Assertions.assertEquals(EventStatus.CANCELLED, found.getStatus());
    }

    @Test
    public void shouldRestoreSoftDeletedMatchAndClearAuditFields() {
        final Match created = createOpenMatch("Original Title", ZonedDateTime.now().plusDays(1), 8);
        final User admin = createUser("admin-restore", "admin-restore@test.com");
        em.flush();
        em.clear();

        matchDao.softDeleteMatch(created.getId(), admin, "Violation");
        em.flush();
        em.clear();

        final boolean restored = matchDao.restoreMatch(created.getId());

        em.flush();
        em.clear();
        final Match found = matchDao.findById(created.getId()).orElseThrow();

        Assertions.assertTrue(restored);
        Assertions.assertFalse(found.isDeleted());
        Assertions.assertNull(found.getDeletedByUser());
        Assertions.assertNull(found.getDeleteReason());
        Assertions.assertNull(found.getDeletedAt());
        Assertions.assertEquals(EventStatus.CANCELLED, found.getStatus());
    }

    @Test
    public void shouldReturnFalseWhenRestoringNonExistingMatch() {
        final boolean restored = matchDao.restoreMatch(999_999L);

        Assertions.assertFalse(restored);
    }

    @Test
    public void testFindHostedMatchesReturnsAllStatusesOrderedBySoonest() {
        createMatchWithPolicy(
                "Host Draft",
                EventStatus.DRAFT,
                ZonedDateTime.now().plusDays(2),
                host,
                EventVisibility.PRIVATE,
                EventJoinPolicy.INVITE_ONLY);
        createMatchWithPolicy(
                "Host Completed",
                EventStatus.COMPLETED,
                ZonedDateTime.now().plusDays(1),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        createMatchWithPolicy(
                "Host Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(3),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        em.flush();
        em.clear();

        final List<Match> matches =
                matchDao.findHostedMatches(
                        host,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(3, matches.size());
        Assertions.assertEquals("Host Completed", matches.get(0).getTitle());
        Assertions.assertEquals("Host Draft", matches.get(1).getTitle());
        Assertions.assertEquals("Host Open", matches.get(2).getTitle());
        Assertions.assertEquals(
                3,
                matchDao.countHostedMatches(
                        host,
                        null,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindHostedMatchesFiltersFinishedStatuses() {
        createMatchWithPolicy(
                "Host Completed",
                EventStatus.COMPLETED,
                ZonedDateTime.now().minusDays(2),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        createMatchWithPolicy(
                "Host Cancelled",
                EventStatus.CANCELLED,
                ZonedDateTime.now().minusDays(1),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        createMatchWithPolicy(
                "Host Open Past",
                EventStatus.OPEN,
                ZonedDateTime.now().minusHours(2),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        createMatchWithPolicy(
                "Host Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(1),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        em.flush();
        em.clear();

        final List<Match> finished =
                matchDao.findHostedMatches(
                        host,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(3, finished.size());
        Assertions.assertTrue(
                finished.stream()
                        .allMatch(
                                m ->
                                        EventStatus.COMPLETED.equals(m.getStatus())
                                                || EventStatus.CANCELLED.equals(m.getStatus())));
        Assertions.assertTrue(
                finished.stream().anyMatch(m -> "Host Open Past".equals(m.getTitle())));
        Assertions.assertEquals(
                3,
                matchDao.countHostedMatches(
                        host,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindHostedMatchesAppliesPriceFilter() {
        createMatchWithPolicyAndPrice(
                "Hosted Budget",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                BigDecimal.ZERO);
        createMatchWithPolicyAndPrice(
                "Hosted Premium",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                new BigDecimal("30"));
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findHostedMatches(
                        host,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(EventStatus.OPEN),
                        null,
                        new BigDecimal("20"),
                        new BigDecimal("40"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Hosted Premium", result.get(0).getTitle());
    }

    @Test
    public void testFindUpcomingJoinedMatchesIncludesCancelledEvents() {
        final User player = createUser("joined-player", "joined-player@test.com");
        final Match openMatch =
                createMatchWithPolicy(
                        "Open Future",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        final Match cancelledMatch =
                createMatchWithPolicy(
                        "Cancelled Future",
                        EventStatus.CANCELLED,
                        ZonedDateTime.now().plusDays(3),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        joinPlayerById(openMatch.getId(), player.getId(), "joined");
        joinPlayerById(cancelledMatch.getId(), player.getId(), "joined");
        em.flush();
        em.clear();

        final List<Match> upcoming =
                matchDao.findJoinedMatches(
                        player,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, upcoming.size());
        Assertions.assertTrue(
                upcoming.stream().anyMatch(m -> EventStatus.CANCELLED.equals(m.getStatus())));
        Assertions.assertEquals(
                2,
                matchDao.countJoinedMatches(
                        player,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindPastJoinedMatchesReturnsOnlyPastInDescendingOrder() {
        final User player = createUser("past-player", "past-player@test.com");
        final Match olderPast =
                createMatchWithPolicy(
                        "Older Past",
                        EventStatus.COMPLETED,
                        ZonedDateTime.now().minusDays(4),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        final Match newerPast =
                createMatchWithPolicy(
                        "Newer Past",
                        EventStatus.OPEN,
                        ZonedDateTime.now().minusDays(2),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT);
        createMatchWithPolicy(
                "Future Open",
                EventStatus.OPEN,
                ZonedDateTime.now().plusDays(2),
                host,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT);
        joinPlayerById(olderPast.getId(), player.getId(), "checked_in");
        joinPlayerById(newerPast.getId(), player.getId(), "joined");
        em.flush();
        em.clear();

        final List<Match> past =
                matchDao.findJoinedMatches(
                        player,
                        Boolean.FALSE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(2, past.size());
        Assertions.assertEquals("Newer Past", past.get(0).getTitle());
        Assertions.assertEquals("Older Past", past.get(1).getTitle());
        Assertions.assertEquals(EventStatus.COMPLETED, past.get(0).getStatus());
        Assertions.assertEquals(
                2,
                matchDao.countJoinedMatches(
                        player,
                        Boolean.FALSE,
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        ZoneId.systemDefault()));
    }

    @Test
    public void testFindUpcomingJoinedMatchesAppliesPriceFilter() {
        final User player = createUser("price-player", "price-player@test.com");
        final Match budget =
                createMatchWithPolicyAndPrice(
                        "Upcoming Budget",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        BigDecimal.ZERO);
        final Match premium =
                createMatchWithPolicyAndPrice(
                        "Upcoming Premium",
                        EventStatus.OPEN,
                        ZonedDateTime.now().plusDays(2),
                        host,
                        EventVisibility.PUBLIC,
                        EventJoinPolicy.DIRECT,
                        new BigDecimal("25"));
        joinPlayerById(budget.getId(), player.getId(), "joined");
        joinPlayerById(premium.getId(), player.getId(), "joined");
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findJoinedMatches(
                        player,
                        Boolean.TRUE,
                        null,
                        List.of(),
                        null,
                        null,
                        EventTimeFilter.WEEK,
                        new BigDecimal("20"),
                        new BigDecimal("30"),
                        MatchSort.SOONEST,
                        ZoneId.systemDefault(),
                        0,
                        20);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Upcoming Premium", result.get(0).getTitle());
    }

    @Test
    public void shouldPagePublicMatches() {
        final Match first = createOpenMatch("First", ZonedDateTime.now().plusDays(1), 5);
        final Match second = createOpenMatch("Second", ZonedDateTime.now().plusDays(2), 5);
        final Match third = createOpenMatch("Third", ZonedDateTime.now().plusDays(3), 5);
        final Match fourth = createOpenMatch("Fourth", ZonedDateTime.now().plusDays(4), 5);
        joinPlayers(first.getId(), 1);
        joinPlayers(second.getId(), 2);
        joinPlayers(third.getId(), 3);
        joinPlayers(fourth.getId(), 4);
        em.flush();
        em.clear();

        final List<Match> result =
                matchDao.findPublicMatches(
                        null,
                        List.of(Sport.FOOTBALL),
                        EventTimeFilter.WEEK,
                        null,
                        null,
                        MatchSort.SPOTS_DESC,
                        ZoneId.systemDefault(),
                        0,
                        3);

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(
                List.of(first.getId(), second.getId(), third.getId()),
                result.stream().map(Match::getId).toList());
        Assertions.assertEquals(4, countMatches());
        Assertions.assertEquals(10, countParticipants());
    }

    private Match createOpenMatch(
            final String title, final ZonedDateTime startsAt, final int maxPlayers) {
        return matchDao.createMatch(
                host,
                "Test Address",
                title,
                "Description",
                startsAt.toInstant(),
                null,
                maxPlayers,
                BigDecimal.ZERO,
                Sport.FOOTBALL,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                null);
    }

    private Match createOpenPublicMatch(
            final String title,
            final String description,
            final Sport sport,
            final int maxPlayers,
            final ZonedDateTime startsAt) {
        return matchDao.createMatch(
                host,
                "Test Address",
                title,
                description,
                startsAt.toInstant(),
                null,
                maxPlayers,
                BigDecimal.ZERO,
                sport,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                null);
    }

    private Match createOpenPublicMatchWithPrice(
            final String title,
            final String description,
            final Sport sport,
            final int maxPlayers,
            final ZonedDateTime startsAt,
            final BigDecimal price) {
        return matchDao.createMatch(
                host,
                "Test Address",
                title,
                description,
                startsAt.toInstant(),
                null,
                maxPlayers,
                price,
                sport,
                EventVisibility.PUBLIC,
                EventJoinPolicy.DIRECT,
                EventStatus.OPEN,
                null);
    }

    private Match createMatchWithPolicy(
            final String title,
            final EventStatus status,
            final ZonedDateTime startsAt,
            final User host,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy) {
        return createMatchWithPolicyAndPrice(
                title, status, startsAt, host, visibility, joinPolicy, BigDecimal.ZERO);
    }

    private Match createMatchWithPolicyAndPrice(
            final String title,
            final EventStatus status,
            final ZonedDateTime startsAt,
            final User host,
            final EventVisibility visibility,
            final EventJoinPolicy joinPolicy,
            final BigDecimal pricePerPlayer) {
        return matchDao.createMatch(
                host,
                "Test Address",
                title,
                "Description",
                startsAt.toInstant(),
                null,
                10,
                pricePerPlayer,
                Sport.FOOTBALL,
                visibility,
                joinPolicy,
                status,
                null);
    }

    private void joinPlayers(final long matchId, final int players) {
        for (int i = 0; i < players; i++) {
            final long userId = (matchId * 100L) + i;
            em.createNativeQuery(
                            "INSERT INTO users (id, username, email, created_at, updated_at)"
                                    + " VALUES (:userId, :username, :email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                    .setParameter("userId", userId)
                    .setParameter("username", "user" + userId)
                    .setParameter("email", "user" + userId + "@test.com")
                    .executeUpdate();
            em.createNativeQuery(
                            "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                    + " VALUES (:matchId, :userId, 'joined', CURRENT_TIMESTAMP)")
                    .setParameter("matchId", matchId)
                    .setParameter("userId", userId)
                    .executeUpdate();
        }
    }

    private void joinPlayerById(final long matchId, final long userId, final String status) {
        em.createNativeQuery(
                        "INSERT INTO match_participants (match_id, user_id, status, joined_at)"
                                + " VALUES (:matchId, :userId, :status, CURRENT_TIMESTAMP)")
                .setParameter("matchId", matchId)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .executeUpdate();
    }

    private User createUser(final String username, final String email) {
        final long userId = System.nanoTime();
        em.createNativeQuery(
                        "INSERT INTO users (id, username, email, created_at, updated_at)"
                                + " VALUES (:userId, :username, :email, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("userId", userId)
                .setParameter("username", username)
                .setParameter("email", email)
                .executeUpdate();
        em.flush();
        em.clear();
        return new User(userId, email, username, null, null, null, null, null);
    }

    private List<Match> findPublicMatchesByQuery(final String query) {
        return matchDao.findPublicMatches(
                query,
                List.of(),
                EventTimeFilter.WEEK,
                null,
                null,
                MatchSort.SOONEST,
                ZoneId.systemDefault(),
                0,
                20);
    }

    private int countMatches() {
        return ((Number) em.createQuery("SELECT COUNT(m) FROM Match m").getSingleResult())
                .intValue();
    }

    private int countParticipants() {
        return ((Number)
                        em.createQuery("SELECT COUNT(mp) FROM MatchParticipant mp")
                                .getSingleResult())
                .intValue();
    }
}
