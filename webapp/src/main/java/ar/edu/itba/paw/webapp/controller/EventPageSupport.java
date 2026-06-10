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
import ar.edu.itba.paw.models.types.ReportTargetType;
import ar.edu.itba.paw.services.MatchActionCapabilities;
import ar.edu.itba.paw.services.MatchInteractionState;
import ar.edu.itba.paw.services.MatchManagementPermissions;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import java.time.Clock;
import java.time.Instant;
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
    private final MatchParticipationService matchParticipationService;
    private final PlayerReviewService playerReviewService;
    private final ModerationService moderationService;
    private final MessageSource messageSource;
    private final Clock clock;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final int mapDefaultZoom;

    EventPageSupport(
            final MatchService matchService,
            final MatchParticipationService matchParticipationService,
            final PlayerReviewService playerReviewService,
            final ModerationService moderationService,
            final MessageSource messageSource,
            final Clock clock,
            final boolean mapPickerEnabled,
            final String mapTileUrlTemplate,
            final String mapAttribution,
            final int mapDefaultZoom) {
        this.matchService = matchService;
        this.matchParticipationService = matchParticipationService;
        this.playerReviewService = playerReviewService;
        this.moderationService = moderationService;
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
                        .findVisibleMatchById(eventId, currentUser)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final MatchActionCapabilities matchActionCapabilities =
                matchService.actionCapabilities(match, currentUser);

        if (!matchActionCapabilities.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final boolean isApprovalRequired =
                match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
        final boolean isInviteOnly = match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
        final boolean isPrivateEvent = match.getVisibility() == EventVisibility.PRIVATE;

        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final MatchManagementPermissions managementPermissions =
                matchService.getMatchManagementPermissions(match, currentUser);
        final boolean isHost = managementPermissions.isHostViewer();
        final boolean hostCanManageParticipants = managementPermissions.canManageParticipants();
        final List<User> pendingHostRequests =
                hostCanManageParticipants && isApprovalRequired
                        ? matchParticipationService.findPendingRequests(eventId, currentUser)
                        : List.of();
        final List<User> pendingHostInvites =
                hostCanManageParticipants && isInviteOnly
                        ? matchParticipationService.findInvitedUsers(eventId, currentUser)
                        : List.of();
        final List<User> declinedHostInvites =
                hostCanManageParticipants && isInviteOnly
                        ? matchParticipationService.findDeclinedInvitees(eventId, currentUser)
                        : List.of();
        final PaginatedResult<Match> seriesOccurrencesPage =
                match.isRecurringOccurrence()
                        ? matchService.findSeriesOccurrencesPage(
                                match.getSeries().getId(), seriesPage, SERIES_PAGE_SIZE)
                        : new PaginatedResult<>(List.of(), 0, 1, SERIES_PAGE_SIZE);
        final List<Match> seriesOccurrences = seriesOccurrencesPage.getItems();
        final MatchInteractionState interactionState =
                matchService.getMatchInteractionState(match, seriesOccurrences, currentUser);
        final boolean suppressReservationErrors =
                interactionState.hasPendingJoinRequest()
                        || interactionState.isSeriesJoinRequestPending();
        final ModelAndView mav = new ModelAndView("matches/detail");
        mav.addObject("matchActionCapabilities", matchActionCapabilities);
        mav.addObject("isConfirmedParticipant", interactionState.isConfirmedParticipant());
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("isInviteOnly", isInviteOnly);
        final boolean reportMatchVisible = canReportMatch(currentUser, match, isHost);
        final boolean reportMatchAlreadySubmitted =
                reportMatchVisible
                        && moderationService.hasReportedTarget(
                                currentUser, ReportTargetType.MATCH, match.getId());
        mav.addObject("reportMatchVisible", reportMatchVisible);
        mav.addObject("reportMatchAlreadySubmitted", reportMatchAlreadySubmitted);
        mav.addObject("reportMatchCanSubmit", reportMatchVisible && !reportMatchAlreadySubmitted);
        mav.addObject("reservationRequiresLogin", interactionState.isReservationRequiresLogin());
        addRealEventPageAttributes(
                mav, match, confirmedParticipants, seriesOccurrences, currentUser);
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

        mav.addObject("reservationEnabled", interactionState.isReservationEnabled());
        mav.addObject("reservationRequestPath", "/matches/" + eventId + "/reservations");
        mav.addObject("reservationCancelPath", "/matches/" + eventId + "/reservations/cancel");
        mav.addObject(
                "reservationCancellationEnabled",
                interactionState.isReservationCancellationEnabled());
        mav.addObject("reservationError", suppressReservationErrors ? null : reservationError);
        mav.addObject(
                "reservationConfirmed",
                interactionState.isConfirmedParticipant()
                        && "confirmed".equalsIgnoreCase(reservationStatus));
        mav.addObject("reservationCancelled", "cancelled".equalsIgnoreCase(reservationStatus));
        mav.addObject("seriesReservationPath", "/matches/" + eventId + "/recurring-reservations");
        mav.addObject(
                "seriesReservationCancelPath",
                "/matches/" + eventId + "/recurring-reservations/cancel");
        mav.addObject("seriesReservationEnabled", interactionState.isSeriesReservationEnabled());
        mav.addObject("seriesReservationJoined", interactionState.isSeriesReservationJoined());
        mav.addObject("seriesCancellationEnabled", interactionState.isSeriesCancellationEnabled());
        mav.addObject(
                "seriesReservationRequiresLogin",
                interactionState.isSeriesReservationRequiresLogin());
        mav.addObject(
                "seriesReservationConfirmed",
                interactionState.isSeriesReservationJoined()
                        && ("recurringConfirmed".equalsIgnoreCase(reservationStatus)
                                || "seriesConfirmed".equalsIgnoreCase(reservationStatus)));
        mav.addObject(
                "seriesReservationCancelled",
                "recurringCancelled".equalsIgnoreCase(reservationStatus)
                        || "seriesCancelled".equalsIgnoreCase(reservationStatus));
        mav.addObject(
                "seriesReservationError",
                suppressReservationErrors ? null : seriesReservationError);
        mav.addObject("eventStateNoticeCode", eventStateNoticeCode(match));

        mav.addObject("joinRequestEnabled", interactionState.isJoinRequestEnabled());
        mav.addObject("joinRequestPath", "/matches/" + eventId + "/join-requests");
        mav.addObject("seriesJoinRequestPath", "/matches/" + eventId + "/recurring-join-requests");
        mav.addObject("seriesJoinRequestEnabled", interactionState.isSeriesJoinRequestEnabled());
        mav.addObject("seriesJoinRequestPending", interactionState.isSeriesJoinRequestPending());
        mav.addObject(
                "seriesJoinRequestRequiresLogin",
                interactionState.isSeriesJoinRequestRequiresLogin());
        mav.addObject("cancelJoinRequestPath", "/matches/" + eventId + "/join-requests/cancel");
        mav.addObject("hasPendingJoinRequest", interactionState.hasPendingJoinRequest());
        mav.addObject(
                "joinRequested", joinRequestedFlash || "requested".equalsIgnoreCase(joinStatus));
        mav.addObject(
                "seriesJoinRequested",
                seriesJoinRequestedFlash || "recurringRequested".equalsIgnoreCase(joinStatus));
        mav.addObject("joinCancelled", "cancelled".equalsIgnoreCase(joinStatus));
        mav.addObject("joinError", joinError);

        mav.addObject("isInvitedPlayer", interactionState.isInvitedPlayer());
        mav.addObject("acceptInvitePath", "/matches/" + eventId + "/invites/accept");
        mav.addObject("declineInvitePath", "/matches/" + eventId + "/invites/decline");
        mav.addObject("inviteAccepted", "accepted".equalsIgnoreCase(inviteStatus));
        mav.addObject("inviteError", inviteError);

        mav.addObject("hostViewer", isHost);
        mav.addObject("isPrivateEvent", isPrivateEvent);
        mav.addObject("hostCanManage", managementPermissions.canManage());
        mav.addObject("hostCanManageParticipants", hostCanManageParticipants);
        mav.addObject("hostCanEdit", managementPermissions.canEdit());
        mav.addObject("hostCanCancel", managementPermissions.canCancel());
        mav.addObject("hostCanEditSeries", managementPermissions.canEditSeries());
        mav.addObject("hostCanCancelSeries", managementPermissions.canCancelSeries());
        mav.addObject("hostEditPath", "/host/matches/" + eventId + "/edit");
        mav.addObject("hostCancelPath", "/host/matches/" + eventId + "/cancel");
        mav.addObject("hostSeriesEditPath", "/host/matches/" + eventId + "/series/edit");
        mav.addObject("hostSeriesCancelPath", "/host/matches/" + eventId + "/series/cancel");
        mav.addObject("hostActionCode", hostAction);
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

    private static boolean canReportMatch(
            final User currentUser, final Match match, final boolean isHost) {
        return currentUser != null && match != null && !isHost;
    }

    private void addRealEventPageAttributes(
            final ModelAndView mav,
            final Match match,
            final List<User> confirmedParticipants,
            final List<Match> seriesOccurrences,
            final User currentUser) {
        final User host = match.getHost();
        mav.addObject("event", match);
        mav.addObject("eventMediaClass", mediaClassFor(match.getSport()));
        mav.addObject("eventBannerImageUrl", bannerUrlFor(match));
        mav.addObject("hostProfileHref", profileHrefFor(host));
        mav.addObject("hostUsername", hostUsername(host));
        mav.addObject("unknownHostArgument", unknownHostArgument(host));
        mav.addObject("hostProfileImageUrl", profileUrlFor(host));
        mav.addObject("participants", confirmedParticipants);
        mav.addObject("aboutParagraphs", buildAboutParagraphs(match));
        mav.addObject("nearbyEvents", List.of());
        mav.addObject("occurrences", seriesOccurrences);
        mav.addObject(
                "occurrenceVisibleHrefs", occurrenceVisibleHrefs(seriesOccurrences, currentUser));
        mav.addObject("occurrenceDisplayStateKeys", occurrenceDisplayStateKeys(seriesOccurrences));
        mav.addObject("occurrenceStatusTones", occurrenceStatusTones(seriesOccurrences));
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

    private static List<String> buildAboutParagraphs(final Match match) {
        if (match.getDescription() == null || match.getDescription().isBlank()) {
            return List.of();
        }
        return List.of(normalizeDescriptionLineBreaks(match.getDescription()));
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
        return user == null || user.getUsername() == null || user.getUsername().isBlank()
                ? null
                : "/users/" + user.getUsername();
    }

    private static String hostUsername(final User host) {
        if (host == null || host.getUsername() == null || host.getUsername().isBlank()) {
            return null;
        }
        return host.getUsername();
    }

    private static String unknownHostArgument(final User host) {
        return host == null || host.getId() == null ? "?" : String.valueOf(host.getId());
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

    private String eventStateNoticeCode(final Match match) {
        final EventDisplayState state = eventDisplayState(match);
        if ("completed".equals(state.key()) || "inProgress".equals(state.key())) {
            return "event.state.completedNotice";
        }
        if ("cancelled".equals(state.key())) {
            return "event.state.cancelledNotice";
        }
        return null;
    }

    private boolean hasEventEnded(final Match match) {
        final Instant endsAt = match.getEndsAt() == null ? match.getStartsAt() : match.getEndsAt();
        return !endsAt.isAfter(Instant.now(clock));
    }

    private boolean isEventInProgress(final Match match) {
        final Instant now = Instant.now(clock);
        return match.getEndsAt() != null
                && !match.getStartsAt().isAfter(now)
                && match.getEndsAt().isAfter(now);
    }

    private String reservationErrorMessage(final String code, final Locale locale) {
        if (code == null) {
            return null;
        }
        final String errorKey = "reservation.error." + code;
        return messageSource.getMessage(errorKey, null, locale);
    }

    private String joinErrorMessage(final String code, final Locale locale) {
        if (code == null) {
            return null;
        }
        return messageSource.getMessage("join.error." + code, null, locale);
    }

    private String inviteErrorMessage(final String code, final Locale locale) {
        if (code == null) {
            return null;
        }
        return messageSource.getMessage("invite.error." + code, null, locale);
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

    private record EventDisplayState(String key, String tone) {}
}
