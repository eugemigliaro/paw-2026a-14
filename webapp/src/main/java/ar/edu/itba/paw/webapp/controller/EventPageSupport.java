package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.EventCardViewModelUtils.toCard;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.dateFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.scheduleFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.timeFormatter;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.types.EventJoinPolicy;
import ar.edu.itba.paw.models.types.EventStatus;
import ar.edu.itba.paw.models.types.EventVisibility;
import ar.edu.itba.paw.services.MatchInteractionState;
import ar.edu.itba.paw.services.MatchManagementPermissions;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.BookingDetailViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventDetailPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventOccurrenceViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.InviteParticipantViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.ParticipantViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.PendingRequestViewModel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
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
        final User currentUser = SecurityControllerUtils.currentUserOrNull();
        final Match match =
                matchService
                        .findVisibleMatchById(eventId, currentUser)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final boolean isApprovalRequired =
                match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
        final boolean isInviteOnly = match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
        final boolean isPrivateEvent = match.getVisibility() == EventVisibility.PRIVATE;

        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final MatchManagementPermissions managementPermissions =
                matchService.getMatchManagementPermissions(match, currentUser);
        final boolean isHostViewer = managementPermissions.isHostViewer();
        final boolean hostCanManageParticipants = managementPermissions.canManageParticipants();
        final List<User> pendingHostRequests =
                isHostViewer && isApprovalRequired
                        ? matchParticipationService.findPendingRequests(eventId, currentUser)
                        : List.of();
        final List<User> pendingHostInvites =
                isHostViewer && isInviteOnly
                        ? matchParticipationService.findInvitedUsers(eventId, currentUser)
                        : List.of();
        final List<User> declinedHostInvites =
                isHostViewer && isInviteOnly
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
        mav.addObject("isConfirmedParticipant", interactionState.isConfirmedParticipant());
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("isInviteOnly", isInviteOnly);
        mav.addObject("reservationRequiresLogin", interactionState.isReservationRequiresLogin());
        mav.addObject(
                "eventPage",
                buildRealEventPage(
                        match,
                        confirmedParticipants,
                        seriesOccurrences,
                        currentUser,
                        locale,
                        hostCanManageParticipants));
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
        mav.addObject("eventStateNotice", eventStateNotice(match, locale));

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

        mav.addObject("hostViewer", isHostViewer);
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
        mav.addObject("hostActionNotice", hostActionNotice(hostAction, locale));
        mav.addObject("hostActionErrorNotice", hostActionError);
        mav.addObject("hostActionTarget", hostActionTarget);
        mav.addObject(
                "hostPendingRequests", toPendingRequestViewModels(pendingHostRequests, eventId));
        mav.addObject("hostPendingRequestCount", pendingHostRequests.size());
        mav.addObject(
                "hostPendingRequestsOpen",
                !pendingHostRequests.isEmpty()
                        || isRequestHostAction(hostAction)
                        || "requests".equalsIgnoreCase(hostActionTarget));
        mav.addObject("hostPendingInvites", toInviteParticipantViewModels(pendingHostInvites));
        mav.addObject("hostDeclinedInvites", toInviteParticipantViewModels(declinedHostInvites));
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

    private EventDetailPageViewModel buildRealEventPage(
            final Match match,
            final List<User> confirmedParticipants,
            final List<Match> seriesOccurrences,
            final User currentUser,
            final Locale locale,
            final boolean includeHostParticipantActions) {
        final User host = match.getHost();
        final Set<Long> reviewableUserIds =
                currentUser == null
                        ? Set.of()
                        : Optional.ofNullable(
                                        playerReviewService.findReviewableUserIds(currentUser))
                                .orElseGet(Set::of);
        return new EventDetailPageViewModel(
                toCard(
                        match,
                        ZoneId.systemDefault(),
                        locale,
                        currentUser,
                        buildAvailabilityLabel(match, locale),
                        messageSource,
                        matchParticipationService,
                        matchReservationService),
                null,
                null,
                host.getUsername() != null
                        ? host.getUsername()
                        : messageSource.getMessage(
                                "event.detail.unknownHost",
                                new Object[] {match.getHost().getId()},
                                locale),
                profileHrefFor(host),
                profileUrlFor(host),
                toParticipantViewModels(
                        confirmedParticipants,
                        match.getId(),
                        currentUser,
                        reviewableUserIds,
                        includeHostParticipantActions),
                buildParticipantCountLabel(confirmedParticipants.size(), locale),
                messageSource.getMessage("event.detail.noPlayersHint", null, locale),
                buildAboutParagraphs(match, locale),
                priceLabel(match.getPricePerPlayer(), locale, messageSource),
                buildBookingDetails(match, locale),
                buildAvailabilityLabel(match, locale),
                messageSource.getMessage("event.booking.cta", null, locale),
                List.of(),
                toOccurrenceViewModels(match, seriesOccurrences, currentUser, locale),
                mapPickerEnabled && !mapTileUrlTemplate.isBlank() && match.hasCoordinates(),
                match.getLatitude(),
                match.getLongitude(),
                mapTileUrlTemplate,
                mapAttribution,
                mapDefaultZoom);
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

    private List<BookingDetailViewModel> buildBookingDetails(
            final Match match, final Locale locale) {
        return List.of(
                new BookingDetailViewModel(
                        messageSource.getMessage("event.booking.date", null, locale),
                        dateFormatter(locale)
                                .format(match.getStartsAt().atZone(ZoneId.systemDefault()))),
                new BookingDetailViewModel(
                        messageSource.getMessage("event.booking.time", null, locale),
                        timeFormatter(locale)
                                        .format(match.getStartsAt().atZone(ZoneId.systemDefault()))
                                + (match.getEndsAt() == null
                                        ? ""
                                        : " - "
                                                + timeFormatter(locale)
                                                        .format(
                                                                match.getEndsAt()
                                                                        .atZone(
                                                                                ZoneId
                                                                                        .systemDefault())))),
                new BookingDetailViewModel(
                        messageSource.getMessage("event.booking.venue", null, locale),
                        match.getAddress()));
    }

    private List<ParticipantViewModel> toParticipantViewModels(
            final List<User> confirmedParticipants,
            final long matchId,
            final User currentUser,
            final Set<Long> reviewableUserIds,
            final boolean includeHostParticipantActions) {
        return confirmedParticipants.stream()
                .map(
                        participant ->
                                new ParticipantViewModel(
                                        participant.getUsername(),
                                        avatarLabelForUsername(participant.getUsername()),
                                        profileHrefFor(participant),
                                        profileImageUrlForParticipant(participant),
                                        reviewHrefForParticipant(
                                                participant, currentUser, reviewableUserIds),
                                        includeHostParticipantActions && participant.getId() != null
                                                ? "/host/matches/"
                                                        + matchId
                                                        + "/participants/"
                                                        + participant.getId()
                                                        + "/remove"
                                                : null))
                .toList();
    }

    private List<PendingRequestViewModel> toPendingRequestViewModels(
            final List<User> users, final long matchId) {
        return users.stream()
                .map(
                        user ->
                                new PendingRequestViewModel(
                                        user.getUsername(),
                                        avatarLabelForUsername(user.getUsername()),
                                        "/host/matches/"
                                                + matchId
                                                + "/requests/"
                                                + user.getId()
                                                + "/approve",
                                        "/host/matches/"
                                                + matchId
                                                + "/requests/"
                                                + user.getId()
                                                + "/reject",
                                        profileHrefFor(user),
                                        profileImageUrlForParticipant(user),
                                        null,
                                        null,
                                        false))
                .toList();
    }

    private List<InviteParticipantViewModel> toInviteParticipantViewModels(final List<User> users) {
        return users.stream()
                .map(
                        user ->
                                new InviteParticipantViewModel(
                                        user.getUsername(),
                                        avatarLabelForUsername(user.getUsername()),
                                        profileHrefFor(user),
                                        profileImageUrlForParticipant(user)))
                .toList();
    }

    private String reviewHrefForParticipant(
            final User participant, final User currentUser, final Set<Long> reviewableUserIds) {
        if (currentUser == null
                || participant.getId() == null
                || !reviewableUserIds.contains(participant.getId())) {
            return null;
        }
        return "/users/" + participant.getUsername() + "?reviewForm=open#reviews";
    }

    private String profileImageUrlForParticipant(final User participant) {
        return profileUrlFor(participant);
    }

    private String profileHrefFor(final User user) {
        return user.getUsername() == null ? null : "/users/" + user.getUsername();
    }

    private List<EventOccurrenceViewModel> toOccurrenceViewModels(
            final Match currentMatch,
            final List<Match> occurrences,
            final User currentUser,
            final Locale locale) {
        return occurrences.stream()
                .map(
                        occurrence -> {
                            final EventDisplayState state = eventDisplayState(occurrence);
                            final String href =
                                    matchService.canViewMatch(occurrence, currentUser)
                                            ? "/matches/" + occurrence.getId()
                                            : null;
                            return new EventOccurrenceViewModel(
                                    href,
                                    scheduleFormatter(locale)
                                            .format(
                                                    occurrence
                                                            .getStartsAt()
                                                            .atZone(ZoneId.systemDefault())),
                                    eventStateLabel(state, locale),
                                    state.tone(),
                                    occurrence.getId().equals(currentMatch.getId()),
                                    buildSpotsLabel(occurrence, state, locale),
                                    buildSpotsTone(occurrence, state));
                        })
                .toList();
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

    private String eventStateLabel(final EventDisplayState state, final Locale locale) {
        return messageSource.getMessage("match.status." + state.key(), null, locale);
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
        if ("completed".equals(state.key())) {
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

    private boolean isEventInProgress(final Match match) {
        final Instant now = Instant.now(clock);
        return match.getEndsAt() != null
                && !match.getStartsAt().isAfter(now)
                && match.getEndsAt().isAfter(now);
    }

    private String buildAvailabilityLabel(final Match match, final Locale locale) {
        return messageSource.getMessage(
                "event.availability",
                new Object[] {match.getAvailableSpots(), match.getMaxPlayers()},
                locale);
    }

    private String buildParticipantCountLabel(final int participantCount, final Locale locale) {
        return participantCount == 1
                ? messageSource.getMessage("event.participants.one", null, locale)
                : messageSource.getMessage(
                        "event.participants.many", new Object[] {participantCount}, locale);
    }

    private String reservationErrorMessage(final String code, final Locale locale) {
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

    private String joinErrorMessage(final String code, final Locale locale) {
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

    private String inviteErrorMessage(final String code, final Locale locale) {
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

    private String hostActionNotice(final String hostAction, final Locale locale) {
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

    private static boolean isRequestHostAction(final String hostAction) {
        return "requestApproved".equalsIgnoreCase(hostAction)
                || "requestRejected".equalsIgnoreCase(hostAction);
    }

    private static boolean isInviteHostAction(final String hostAction) {
        return "inviteSent".equalsIgnoreCase(hostAction)
                || "seriesInviteSent".equalsIgnoreCase(hostAction);
    }

    private record EventDisplayState(String key, String tone) {}
}
