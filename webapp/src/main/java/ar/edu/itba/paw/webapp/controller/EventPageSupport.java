package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.mediaClassFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.services.MatchActionCapabilities;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

final class EventPageSupport {
    private static final int SERIES_PAGE_SIZE = 5;

    private final MatchService matchService;
    private final MatchReservationService matchReservationService;
    private final MatchParticipationService matchParticipationService;
    private final PlayerReviewService playerReviewService;
    private final MessageSource messageSource;
    private final Clock clock;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final int mapDefaultZoom;

    EventPageSupport(
            final MatchService matchService,
            final MatchReservationService matchReservationService,
            final MatchParticipationService matchParticipationService,
            final PlayerReviewService playerReviewService,
            final MessageSource messageSource,
            final Clock clock,
            final boolean mapPickerEnabled,
            final String mapTileUrlTemplate,
            final String mapAttribution,
            final int mapDefaultZoom) {
        this.matchService = matchService;
        this.matchReservationService = matchReservationService;
        this.matchParticipationService = matchParticipationService;
        this.playerReviewService = playerReviewService;
        this.messageSource = messageSource;
        this.clock = clock;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    ModelAndView showEventDetails(
            final User currentUser,
            final Long eventId,
            final String reservationStatus,
            final String reservationErrorCode,
            final String seriesReservationErrorCode,
            final String hostAction,
            final String hostActionError,
            final String hostActionTarget,
            final String hostInviteEmail,
            final String joinStatus,
            final String joinErrorCode,
            final String inviteStatus,
            final String inviteErrorCode,
            final boolean joinRequestedFlash,
            final boolean seriesJoinRequestedFlash,
            final int seriesPage,
            final Locale locale) {
        return showRealEventDetails(
                currentUser,
                eventId,
                reservationStatus,
                hostAction,
                hostActionError,
                hostActionTarget,
                hostInviteEmail,
                reservationErrorCode == null
                        ? null
                        : reservationErrorMessage(reservationErrorCode, locale),
                seriesReservationErrorCode == null
                        ? null
                        : reservationErrorMessage(seriesReservationErrorCode, locale),
                joinStatus,
                joinRequestedFlash,
                seriesJoinRequestedFlash,
                joinErrorCode == null ? null : joinErrorMessage(joinErrorCode, locale),
                inviteStatus,
                inviteErrorCode == null ? null : inviteErrorMessage(inviteErrorCode, locale),
                seriesPage,
                locale);
    }

    boolean shouldRedirectToPlayerMatchesAfterCancellation(final Match match, final User user) {
        return match != null
                && user != null
                && match.getVisibility() == EventVisibility.PRIVATE
                && !user.getId().equals(match.getHost().getId());
    }

    private ModelAndView showRealEventDetails(
            final User currentUser,
            final Long eventId,
            final String reservationStatus,
            final String hostAction,
            final String hostActionError,
            final String hostActionTarget,
            final String hostInviteEmail,
            final String reservationError,
            final String seriesReservationError,
            final String joinStatus,
            final boolean joinRequestedFlash,
            final boolean seriesJoinRequestedFlash,
            final String joinError,
            final String inviteStatus,
            final String inviteError,
            final int seriesPage,
            final Locale locale) {
        final Match match =
                matchService
                        .findMatchById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final MatchActionCapabilities matchActionCapabilities =
                matchService.actionCapabilities(match, currentUser);

        if (!matchActionCapabilities.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final boolean isHost = isHost(match, currentUser);
        final boolean hasPendingRequest =
                !isHost
                        && currentUser != null
                        && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                        && matchParticipationService.hasPendingRequest(eventId, currentUser);
        final boolean isInvitedPlayer =
                !isHost
                        && currentUser != null
                        && match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY
                        && matchParticipationService.hasInvitation(eventId, currentUser);
        final boolean isConfirmedParticipant =
                currentUser != null
                        && matchReservationService.hasActiveReservation(eventId, currentUser);
        final boolean isApprovalRequired =
                match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
        final boolean isInviteOnly = match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
        final boolean isPrivateEvent = match.getVisibility() == EventVisibility.PRIVATE;

        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final List<User> pendingHostRequests =
                isHost && isApprovalRequired
                        ? matchParticipationService.findPendingRequests(eventId, currentUser)
                        : List.of();
        final List<User> pendingHostInvites =
                isHost && isInviteOnly
                        ? matchParticipationService.findInvitedUsers(eventId, currentUser)
                        : List.of();
        final List<User> declinedHostInvites =
                isHost && isInviteOnly
                        ? matchParticipationService.findDeclinedInvitees(eventId, currentUser)
                        : List.of();
        final PaginatedResult<Match> seriesOccurrencesPage =
                match.isRecurringOccurrence()
                        ? matchService.findSeriesOccurrencesPage(
                                match.getSeries().getId(), seriesPage, SERIES_PAGE_SIZE)
                        : new PaginatedResult<>(List.of(), 0, 1, SERIES_PAGE_SIZE);
        final List<Match> seriesOccurrences = seriesOccurrencesPage.getItems();
        final SeriesReservationUiState seriesReservationState =
                match.isRecurringOccurrence()
                        ? buildSeriesReservationUiState(
                                match.getSeries().getId(), seriesOccurrences, currentUser, isHost)
                        : buildSeriesReservationUiState(
                                null, seriesOccurrences, currentUser, isHost);
        final SeriesJoinRequestUiState seriesJoinRequestState =
                isHost
                        ? new SeriesJoinRequestUiState(false, false)
                        : buildSeriesJoinRequestUiState(match, seriesOccurrences, currentUser);
        final boolean suppressReservationErrors =
                hasPendingRequest || seriesJoinRequestState.pending();
        final ModelAndView mav = new ModelAndView("matches/detail");
        mav.addObject("matchActionCapabilities", matchActionCapabilities);
        mav.addObject("isConfirmedParticipant", isConfirmedParticipant);
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("isInviteOnly", isInviteOnly);
        mav.addObject("reservationRequiresLogin", CurrentAuthenticatedUser.get().isEmpty());
        addRealEventPageAttributes(
                mav, match, confirmedParticipants, seriesOccurrences, currentUser, locale);
        mav.addObject(
                "userProfileImageUrls",
                userProfileImageUrls(
                        confirmedParticipants,
                        pendingHostRequests,
                        pendingHostInvites,
                        declinedHostInvites));
        mav.addObject(
                "participantReviewHrefs",
                participantReviewHrefs(confirmedParticipants, currentUser));
        mav.addObject(
                "participantRemovePaths",
                participantRemovePaths(
                        confirmedParticipants,
                        eventId,
                        matchActionCapabilities.isCanManageParticipants()));
        if (match.isRecurringOccurrence()) {
            mav.addObject("recurrenceHasPreviousPage", seriesOccurrencesPage.hasPrevious());
            mav.addObject("recurrenceHasNextPage", seriesOccurrencesPage.hasNext());
            mav.addObject(
                    "recurrencePreviousPageHref",
                    buildSeriesScheduleUrl(eventId, seriesOccurrencesPage.getPage() - 1));
            mav.addObject(
                    "recurrenceNextPageHref",
                    buildSeriesScheduleUrl(eventId, seriesOccurrencesPage.getPage() + 1));
            mav.addObject(
                    "recurrencePaginationItems",
                    PaginationUtils.buildPaginationItems(
                            seriesOccurrencesPage.getPage(),
                            seriesOccurrencesPage.getTotalPages(),
                            p -> buildSeriesScheduleUrl(eventId, p)));
        }

        mav.addObject("reservationRequestPath", "/matches/" + eventId + "/reservations");
        mav.addObject("reservationCancelPath", "/matches/" + eventId + "/reservations/cancel");
        mav.addObject("reservationError", suppressReservationErrors ? null : reservationError);
        mav.addObject(
                "reservationConfirmed",
                isConfirmedParticipant && "confirmed".equalsIgnoreCase(reservationStatus));
        mav.addObject("reservationCancelled", "cancelled".equalsIgnoreCase(reservationStatus));
        mav.addObject("seriesReservationPath", "/matches/" + eventId + "/recurring-reservations");
        mav.addObject(
                "seriesReservationCancelPath",
                "/matches/" + eventId + "/recurring-reservations/cancel");
        mav.addObject("seriesReservationEnabled", seriesReservationState.available());
        mav.addObject("seriesReservationJoined", seriesReservationState.joined());
        mav.addObject("seriesCancellationEnabled", seriesReservationState.cancellable());
        mav.addObject("seriesReservationRequiresLogin", currentUser == null);
        mav.addObject(
                "seriesReservationConfirmed",
                seriesReservationState.joined()
                        && ("recurringConfirmed".equalsIgnoreCase(reservationStatus)
                                || "seriesConfirmed".equalsIgnoreCase(reservationStatus)));
        mav.addObject(
                "seriesReservationCancelled",
                "recurringCancelled".equalsIgnoreCase(reservationStatus)
                        || "seriesCancelled".equalsIgnoreCase(reservationStatus));
        mav.addObject(
                "seriesReservationError",
                suppressReservationErrors ? null : seriesReservationError);
        mav.addObject("eventStateNotice", eventStateNotice(match, locale));

        mav.addObject("joinRequestPath", "/matches/" + eventId + "/join-requests");
        mav.addObject("seriesJoinRequestPath", "/matches/" + eventId + "/recurring-join-requests");
        mav.addObject("seriesJoinRequestEnabled", seriesJoinRequestState.available());
        mav.addObject("seriesJoinRequestPending", seriesJoinRequestState.pending());
        mav.addObject("seriesJoinRequestRequiresLogin", currentUser == null);
        mav.addObject("cancelJoinRequestPath", "/matches/" + eventId + "/join-requests/cancel");
        mav.addObject(
                "hasPendingJoinRequest", hasPendingRequest && !seriesJoinRequestState.pending());
        mav.addObject(
                "joinRequested", joinRequestedFlash || "requested".equalsIgnoreCase(joinStatus));
        mav.addObject(
                "seriesJoinRequested",
                seriesJoinRequestedFlash || "recurringRequested".equalsIgnoreCase(joinStatus));
        mav.addObject("joinCancelled", "cancelled".equalsIgnoreCase(joinStatus));
        mav.addObject("joinError", joinError);

        mav.addObject("isInvitedPlayer", isInvitedPlayer);
        mav.addObject("acceptInvitePath", "/matches/" + eventId + "/invites/accept");
        mav.addObject("declineInvitePath", "/matches/" + eventId + "/invites/decline");
        mav.addObject("inviteAccepted", "accepted".equalsIgnoreCase(inviteStatus));
        mav.addObject("inviteError", inviteError);

        mav.addObject("hostViewer", isHost);
        mav.addObject("isPrivateEvent", isPrivateEvent);
        mav.addObject("hostCanManage", isHost);
        mav.addObject("hostEditPath", "/host/matches/" + eventId + "/edit");
        mav.addObject("hostCancelPath", "/host/matches/" + eventId + "/cancel");
        mav.addObject("hostSeriesEditPath", "/host/matches/" + eventId + "/series/edit");
        mav.addObject("hostSeriesCancelPath", "/host/matches/" + eventId + "/series/cancel");
        mav.addObject("hostActionNotice", hostActionNotice(hostAction, locale));
        mav.addObject("hostActionErrorNotice", hostActionError);
        mav.addObject("hostActionTarget", hostActionTarget);
        mav.addObject("hostPendingRequests", pendingHostRequests);
        mav.addObject("hostPendingRequestCount", pendingHostRequests.size());
        mav.addObject(
                "hostPendingRequestsOpen",
                !pendingHostRequests.isEmpty()
                        || isRequestHostAction(hostAction)
                        || "requests".equalsIgnoreCase(hostActionTarget));
        mav.addObject("hostPendingInvites", pendingHostInvites);
        mav.addObject("hostDeclinedInvites", declinedHostInvites);
        mav.addObject("hostPendingInviteCount", pendingHostInvites.size());
        mav.addObject(
                "hostPendingInvitesOpen",
                !pendingHostInvites.isEmpty()
                        || !declinedHostInvites.isEmpty()
                        || isInviteHostAction(hostAction)
                        || "invites".equalsIgnoreCase(hostActionTarget));
        mav.addObject("hostInviteActionPath", "/host/matches/" + eventId + "/invites");
        mav.addObject("hostInviteEmail", hostInviteEmail);
        mav.addObject("hostSeriesInviteAvailable", match.isRecurringOccurrence());
        return mav;
    }

    private void addRealEventPageAttributes(
            final ModelAndView mav,
            final Match match,
            final List<User> confirmedParticipants,
            final List<Match> seriesOccurrences,
            final User currentUser,
            final Locale locale) {
        final User host = match.getHost();
        mav.addObject("event", match);
        mav.addObject("eventMediaClass", mediaClassFor(match.getSport()));
        mav.addObject("eventBannerImageUrl", bannerUrlFor(match));
        mav.addObject(
                "hostLabel",
                host.getUsername() != null
                        ? host.getUsername()
                        : messageSource.getMessage(
                                "event.detail.unknownHost",
                                new Object[] {match.getHost().getId()},
                                locale));
        mav.addObject("hostProfileHref", profileHrefFor(host));
        mav.addObject("hostProfileImageUrl", profileUrlFor(host));
        mav.addObject("participants", confirmedParticipants);
        mav.addObject(
                "participantCountLabel",
                buildParticipantCountLabel(confirmedParticipants.size(), locale));
        mav.addObject(
                "participantsEmptyState",
                messageSource.getMessage("event.detail.noPlayersHint", null, locale));
        mav.addObject("aboutParagraphs", buildAboutParagraphs(match, locale));
        mav.addObject("nearbyEvents", List.of());
        mav.addObject("occurrences", seriesOccurrences);
        mav.addObject(
                "occurrenceVisibleHrefs", occurrenceVisibleHrefs(seriesOccurrences, currentUser));
        mav.addObject("occurrenceDisplayStateKeys", occurrenceDisplayStateKeys(seriesOccurrences));
        mav.addObject("occurrenceStatusTones", occurrenceStatusTones(seriesOccurrences));
        mav.addObject("occurrenceSpotsLabels", occurrenceSpotsLabels(seriesOccurrences, locale));
        mav.addObject("occurrenceSpotsTones", occurrenceSpotsTones(seriesOccurrences));
        mav.addObject(
                "mapAvailable",
                mapPickerEnabled && !mapTileUrlTemplate.isBlank() && match.hasCoordinates());
        mav.addObject("mapLatitude", match.getLatitude());
        mav.addObject("mapLongitude", match.getLongitude());
        mav.addObject("mapTileUrlTemplate", mapTileUrlTemplate);
        mav.addObject("mapAttribution", mapAttribution);
        mav.addObject("mapZoom", mapDefaultZoom);
    }

    private static String buildSeriesScheduleUrl(final Long eventId, final int page) {
        return UriComponentsBuilder.fromPath("/matches/" + eventId)
                .queryParam("seriesPage", page)
                .fragment("recurrence-schedule-title")
                .build()
                .encode()
                .toUriString();
    }

    private List<String> buildAboutParagraphs(final Match match, final Locale locale) {
        final String description =
                match.getDescription() == null || match.getDescription().isBlank()
                        ? messageSource.getMessage("event.detail.defaultDescription", null, locale)
                        : match.getDescription();
        return List.of(normalizeDescriptionLineBreaks(description));
    }

    private static String normalizeDescriptionLineBreaks(final String description) {
        return description
                .replace("\\r\\n", "\n")
                .replace("\\n", "\n")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    private String profileHrefFor(final User user) {
        return user.getUsername() == null ? null : "/users/" + user.getUsername();
    }

    @SafeVarargs
    private Map<Long, String> userProfileImageUrls(final List<User>... userGroups) {
        final Map<Long, String> urls = new LinkedHashMap<>();
        for (final List<User> users : userGroups) {
            for (final User user : users) {
                if (user.getId() != null) {
                    urls.put(user.getId(), profileUrlFor(user));
                }
            }
        }
        return urls;
    }

    private Map<Long, String> participantReviewHrefs(
            final List<User> participants, final User currentUser) {
        final Set<Long> reviewableUserIds =
                currentUser == null
                        ? Set.of()
                        : Optional.ofNullable(
                                        playerReviewService.findReviewableUserIds(currentUser))
                                .orElseGet(Set::of);
        final Map<Long, String> hrefs = new LinkedHashMap<>();
        for (final User participant : participants) {
            if (participant.getId() != null
                    && participant.getUsername() != null
                    && reviewableUserIds.contains(participant.getId())) {
                hrefs.put(
                        participant.getId(),
                        "/users/" + participant.getUsername() + "?reviewForm=open#reviews");
            }
        }
        return hrefs;
    }

    private static Map<Long, String> participantRemovePaths(
            final List<User> participants, final long matchId, final boolean includeActions) {
        final Map<Long, String> paths = new LinkedHashMap<>();
        if (!includeActions) {
            return paths;
        }
        for (final User participant : participants) {
            if (participant.getId() != null) {
                paths.put(
                        participant.getId(),
                        "/host/matches/"
                                + matchId
                                + "/participants/"
                                + participant.getId()
                                + "/remove");
            }
        }
        return paths;
    }

    private Map<Long, String> occurrenceVisibleHrefs(
            final List<Match> occurrences, final User currentUser) {
        final Map<Long, String> hrefs = new LinkedHashMap<>();
        for (final Match occurrence : occurrences) {
            if (occurrence.getId() != null
                    && matchService.actionCapabilities(occurrence, currentUser).isVisible()) {
                hrefs.put(occurrence.getId(), "/matches/" + occurrence.getId());
            }
        }
        return hrefs;
    }

    private Map<Long, String> occurrenceDisplayStateKeys(final List<Match> occurrences) {
        final Map<Long, String> keys = new LinkedHashMap<>();
        for (final Match occurrence : occurrences) {
            keys.put(occurrence.getId(), eventDisplayState(occurrence).key());
        }
        return keys;
    }

    private Map<Long, String> occurrenceStatusTones(final List<Match> occurrences) {
        final Map<Long, String> tones = new LinkedHashMap<>();
        for (final Match occurrence : occurrences) {
            tones.put(occurrence.getId(), eventDisplayState(occurrence).tone());
        }
        return tones;
    }

    private Map<Long, String> occurrenceSpotsLabels(
            final List<Match> occurrences, final Locale locale) {
        final Map<Long, String> labels = new LinkedHashMap<>();
        for (final Match occurrence : occurrences) {
            final String label = buildSpotsLabel(occurrence, eventDisplayState(occurrence), locale);
            if (label != null) {
                labels.put(occurrence.getId(), label);
            }
        }
        return labels;
    }

    private Map<Long, String> occurrenceSpotsTones(final List<Match> occurrences) {
        final Map<Long, String> tones = new LinkedHashMap<>();
        for (final Match occurrence : occurrences) {
            final String tone = buildSpotsTone(occurrence, eventDisplayState(occurrence));
            if (tone != null) {
                tones.put(occurrence.getId(), tone);
            }
        }
        return tones;
    }

    private SeriesReservationUiState buildSeriesReservationUiState(
            final Long seriesId,
            final List<Match> occurrences,
            final User currentUser,
            final boolean isHostViewer) {
        if (occurrences == null || occurrences.isEmpty()) {
            return new SeriesReservationUiState(false, false, false);
        }
        final Set<Long> activeFutureReservationMatchIds =
                currentUser == null || seriesId == null
                        ? Set.of()
                        : matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                seriesId, currentUser);
        final SeriesReservationEvaluation evaluation =
                evaluateSeriesReservationTargets(
                        occurrences, currentUser, activeFutureReservationMatchIds, isHostViewer);
        return new SeriesReservationUiState(
                !evaluation.targetMatchIds().isEmpty(),
                evaluation.joined(),
                evaluation.activeFutureReservationCount() > 0);
    }

    private SeriesReservationEvaluation evaluateSeriesReservationTargets(
            final List<Match> occurrences,
            final User currentUser,
            final Set<Long> activeFutureReservationMatchIds,
            final boolean isHostViewer) {
        final List<Long> targetMatchIds = new ArrayList<>();
        int futureOpenOccurrenceCount = 0;
        int joinedFutureOpenOccurrenceCount = 0;
        int activeFutureReservationCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)) {
                continue;
            }

            final boolean alreadyJoined =
                    activeFutureReservationMatchIds.contains(occurrence.getId());
            if (alreadyJoined) {
                activeFutureReservationCount++;
            }

            if (!isSeriesReservationOpenOccurrence(occurrence, isHostViewer)) {
                continue;
            }

            futureOpenOccurrenceCount++;
            if (alreadyJoined) {
                joinedFutureOpenOccurrenceCount++;
                continue;
            }

            if (occurrence.getAvailableSpots() <= 0) {
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean joined =
                currentUser != null
                        && futureOpenOccurrenceCount > 0
                        && joinedFutureOpenOccurrenceCount == futureOpenOccurrenceCount;
        return new SeriesReservationEvaluation(
                List.copyOf(targetMatchIds), joined, activeFutureReservationCount);
    }

    private static boolean isSeriesReservationOpenOccurrence(
            final Match occurrence, final boolean isHostViewer) {
        return EventStatus.OPEN == occurrence.getStatus()
                && (isHostViewer
                        || (occurrence.getVisibility() == EventVisibility.PUBLIC
                                && occurrence.getJoinPolicy() == EventJoinPolicy.DIRECT));
    }

    private SeriesJoinRequestUiState buildSeriesJoinRequestUiState(
            final Match match, final List<Match> occurrences, final User currentUser) {
        if (currentUser != null
                && match.isRecurringOccurrence()
                && matchParticipationService.hasPendingSeriesRequest(match.getId(), currentUser)) {
            return new SeriesJoinRequestUiState(false, true);
        }

        final Set<Long> activeFutureReservationMatchIds =
                currentUser == null || !match.isRecurringOccurrence()
                        ? Set.of()
                        : matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeries().getId(), currentUser);
        final Set<Long> pendingFutureRequestMatchIds =
                currentUser == null || !match.isRecurringOccurrence()
                        ? Set.of()
                        : matchParticipationService.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeries().getId(), currentUser);
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        currentUser,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds);
        return new SeriesJoinRequestUiState(
                !evaluation.targetMatchIds().isEmpty(), evaluation.pending());
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences,
            final User currentUser,
            final Set<Long> activeFutureReservationMatchIds,
            final Set<Long> pendingFutureRequestMatchIds) {
        final List<Long> targetMatchIds = new ArrayList<>();
        int futureOpenApprovalOccurrenceCount = 0;
        int pendingFutureOpenApprovalOccurrenceCount = 0;
        final Instant now = Instant.now(clock);

        for (final Match occurrence : occurrences) {
            if (!occurrence.getStartsAt().isAfter(now)
                    || !isSeriesJoinRequestOpenOccurrence(occurrence)) {
                continue;
            }

            futureOpenApprovalOccurrenceCount++;
            final boolean alreadyJoined =
                    activeFutureReservationMatchIds.contains(occurrence.getId());
            if (alreadyJoined) {
                continue;
            }

            final boolean alreadyPending =
                    pendingFutureRequestMatchIds.contains(occurrence.getId());
            if (alreadyPending) {
                pendingFutureOpenApprovalOccurrenceCount++;
                continue;
            }

            if (occurrence.getAvailableSpots() <= 0) {
                continue;
            }

            targetMatchIds.add(occurrence.getId());
        }

        final boolean pending =
                currentUser != null
                        && futureOpenApprovalOccurrenceCount > 0
                        && pendingFutureOpenApprovalOccurrenceCount
                                == futureOpenApprovalOccurrenceCount;
        return new SeriesJoinRequestEvaluation(List.copyOf(targetMatchIds), pending);
    }

    private static boolean isSeriesJoinRequestOpenOccurrence(final Match occurrence) {
        return occurrence.getVisibility() == EventVisibility.PUBLIC
                && occurrence.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                && EventStatus.OPEN == occurrence.getStatus();
    }

    private EventDisplayState eventDisplayState(final Match match) {
        final Optional<EventStatus> status = Optional.of(match.getStatus());
        if (status.filter(EventStatus.CANCELLED::equals).isPresent()) {
            return new EventDisplayState("cancelled", "cancelled");
        }
        if (status.filter(EventStatus.COMPLETED::equals).isPresent() || hasEventEnded(match)) {
            return new EventDisplayState("completed", "completed");
        }
        if (isEventInProgress(match)) {
            return new EventDisplayState("inProgress", "in-progress");
        }
        if (match.getAvailableSpots() <= 0) {
            return new EventDisplayState("full", "full");
        }
        return new EventDisplayState("open", "open");
    }

    private String buildSpotsLabel(
            final Match occurrence, final EventDisplayState state, final Locale locale) {
        if (!"open".equals(state.key()) && !"inProgress".equals(state.key())) {
            return null;
        }
        final int spots = occurrence.getAvailableSpots();
        if (spots <= 0) {
            return null;
        }
        if (spots == 1) {
            return messageSource.getMessage("event.occurrence.spots.one", null, locale);
        }
        return messageSource.getMessage(
                "event.occurrence.spots.many", new Object[] {spots}, locale);
    }

    private String buildSpotsTone(final Match occurrence, final EventDisplayState state) {
        if (!"open".equals(state.key()) && !"inProgress".equals(state.key())) {
            return null;
        }
        final int spots = occurrence.getAvailableSpots();
        if (spots <= 0) {
            return null;
        }
        if (spots == 1) {
            return "last";
        }
        final int max = occurrence.getMaxPlayers();
        return (double) spots / max <= 0.33 ? "scarce" : "plenty";
    }

    private String eventStateNotice(final Match match, final Locale locale) {
        final EventDisplayState state = eventDisplayState(match);
        if ("completed".equals(state.key()) || "inProgress".equals(state.key())) {
            return messageSource.getMessage("event.state.completedNotice", null, locale);
        }
        if ("cancelled".equals(state.key())) {
            return messageSource.getMessage("event.state.cancelledNotice", null, locale);
        }
        return null;
    }

    private boolean hasEventEnded(final Match match) {
        final Instant endsAt = match.getEndsAt() == null ? match.getStartsAt() : match.getEndsAt();
        return !endsAt.isAfter(Instant.now(clock));
    }

    private boolean hasEventStarted(final Match match) {
        return !match.getStartsAt().isAfter(Instant.now(clock));
    }

    private boolean isEventInProgress(final Match match) {
        final Instant now = Instant.now(clock);
        return match.getEndsAt() != null
                && !match.getStartsAt().isAfter(now)
                && match.getEndsAt().isAfter(now);
    }

    private String buildParticipantCountLabel(final int participantCount, final Locale locale) {
        return participantCount == 1
                ? messageSource.getMessage("event.participants.one", null, locale)
                : messageSource.getMessage(
                        "event.participants.many", new Object[] {participantCount}, locale);
    }

    private String reservationErrorMessage(
            final String code,
            final Locale
                    locale) { // TODO: check. could be rewritten to be "reservation.error." + code
        // in the message key, and then have a default message for unknown
        // codes
        switch (code) {
            case "closed":
                return messageSource.getMessage("reservation.error.closed", null, locale);
            case "started":
                return messageSource.getMessage("reservation.error.started", null, locale);
            case "already_joined":
                return messageSource.getMessage("reservation.error.alreadyJoined", null, locale);
            case "is_host":
                return messageSource.getMessage("reservation.error.isHost", null, locale);
            case "not_joined":
                return messageSource.getMessage("reservation.error.notJoined", null, locale);
            case "not_cancellable":
                return messageSource.getMessage("reservation.error.notCancellable", null, locale);
            case "full":
                return messageSource.getMessage(
                        "reservation.error.fullBeforeConfirm", null, locale);
            case "not_recurring":
                return messageSource.getMessage("reservation.error.notRecurring", null, locale);
            case "series_started":
                return messageSource.getMessage("reservation.error.seriesStarted", null, locale);
            case "series_closed":
                return messageSource.getMessage("reservation.error.seriesClosed", null, locale);
            case "series_already_joined":
                return messageSource.getMessage(
                        "reservation.error.seriesAlreadyJoined", null, locale);
            case "series_full":
                return messageSource.getMessage("reservation.error.seriesFull", null, locale);
            case "series_not_joined":
                return messageSource.getMessage("reservation.error.seriesNotJoined", null, locale);
            case "not_found":
            default:
                return messageSource.getMessage("reservation.error.notFound", null, locale);
        }
    }

    private static String avatarLabelForUsername(final String username) {
        if (username == null || username.isBlank()) {
            return "?";
        }

        final String[] segments = username.trim().split("[^A-Za-z0-9]+");
        if (segments.length >= 2) {
            return (segments[0].substring(0, 1) + segments[1].substring(0, 1)).toUpperCase();
        }

        final String compact = username.replaceAll("[^A-Za-z0-9]", "");
        if (compact.length() >= 2) {
            return compact.substring(0, 2).toUpperCase();
        }
        return compact.substring(0, 1).toUpperCase();
    }

    private String joinErrorMessage(
            final String code, final Locale locale) { // TODO: same obs as reservationErrorMessage
        switch (code) {
            case "closed":
                return messageSource.getMessage("join.error.closed", null, locale);
            case "started":
                return messageSource.getMessage("join.error.started", null, locale);
            case "already_joined":
                return messageSource.getMessage("join.error.alreadyJoined", null, locale);
            case "already_pending":
                return messageSource.getMessage("join.error.alreadyPending", null, locale);
            case "full":
                return messageSource.getMessage("join.error.full", null, locale);
            case "is_host":
                return messageSource.getMessage("join.error.isHost", null, locale);
            case "not_invite_only":
                return messageSource.getMessage("join.error.notInviteOnly", null, locale);
            case "no_pending_request":
                return messageSource.getMessage("join.error.noPendingRequest", null, locale);
            case "not_recurring":
                return messageSource.getMessage("join.error.notRecurring", null, locale);
            case "series_started":
                return messageSource.getMessage("join.error.seriesStarted", null, locale);
            case "series_closed":
                return messageSource.getMessage("join.error.seriesClosed", null, locale);
            case "series_already_joined":
                return messageSource.getMessage("join.error.seriesAlreadyJoined", null, locale);
            case "series_already_pending":
                return messageSource.getMessage("join.error.seriesAlreadyPending", null, locale);
            case "series_full":
                return messageSource.getMessage("join.error.seriesFull", null, locale);
            case "not_found":
            default:
                return messageSource.getMessage("join.error.notFound", null, locale);
        }
    }

    private String inviteErrorMessage(
            final String code, final Locale locale) { // TODO: same obs as reservationErrorMessage
        if (code == null) {
            return null;
        }
        switch (code) {
            case "closed":
                return messageSource.getMessage("invite.error.closed", null, locale);
            case "started":
                return messageSource.getMessage("invite.error.started", null, locale);
            case "no_invitation":
                return messageSource.getMessage("invite.error.noInvitation", null, locale);
            case "is_host":
                return messageSource.getMessage("invite.error.isHost", null, locale);
            case "not_found":
            default:
                return messageSource.getMessage("invite.error.notFound", null, locale);
        }
    }

    private String hostActionNotice(
            final String hostAction,
            final Locale locale) { // TODO: same obs as reservationErrorMessage
        if ("updated".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("host.action.updated", null, locale);
        }
        if ("cancelled".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("host.action.cancelled", null, locale);
        }
        if ("seriesUpdated".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("host.action.seriesUpdated", null, locale);
        }
        if ("seriesCancelled".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("host.action.seriesCancelled", null, locale);
        }
        if ("participantRemoved".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("event.host.participants.removed", null, locale);
        }
        if ("requestApproved".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("event.host.requests.approved", null, locale);
        }
        if ("requestRejected".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("event.host.requests.rejected", null, locale);
        }
        if ("inviteSent".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("event.host.invites.sent", null, locale);
        }
        if ("seriesInviteSent".equalsIgnoreCase(hostAction)) {
            return messageSource.getMessage("event.host.invites.seriesSent", null, locale);
        }
        return null;
    }

    private static boolean isRequestHostAction(
            final String
                    hostAction) { // TODO: use an enum for host actions instead of strings ¿? If
        // changed, change param typing in controller and add enum
        // converter to WebConfig
        return "requestApproved".equalsIgnoreCase(hostAction)
                || "requestRejected".equalsIgnoreCase(hostAction);
    }

    private static boolean isInviteHostAction(
            final String hostAction) { // TODO: same as isRequestHostAction
        return "inviteSent".equalsIgnoreCase(hostAction)
                || "seriesInviteSent".equalsIgnoreCase(hostAction);
    }

    private boolean isHost(final Match match, final User currentUser) {
        return currentUser != null
                && match.getHost() != null
                && currentUser.getId().equals(match.getHost().getId());
    }

    private static final class SeriesReservationUiState {
        private final boolean available;
        private final boolean joined;
        private final boolean cancellable;

        private SeriesReservationUiState(
                final boolean available, final boolean joined, final boolean cancellable) {
            this.available = available;
            this.joined = joined;
            this.cancellable = cancellable;
        }

        boolean available() {
            return available;
        }

        boolean joined() {
            return joined;
        }

        boolean cancellable() {
            return cancellable;
        }
    }

    private record SeriesReservationEvaluation(
            List<Long> targetMatchIds, boolean joined, int activeFutureReservationCount) {}

    private static final class SeriesJoinRequestUiState {
        private final boolean available;
        private final boolean pending;

        private SeriesJoinRequestUiState(final boolean available, final boolean pending) {
            this.available = available;
            this.pending = pending;
        }

        boolean available() {
            return available;
        }

        boolean pending() {
            return pending;
        }
    }

    private record SeriesJoinRequestEvaluation(List<Long> targetMatchIds, boolean pending) {}

    private record EventDisplayState(String key, String tone) {}
}
