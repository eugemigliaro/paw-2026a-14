package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.EventCardViewModelUtils.toCard;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.DEFAULT_PROFILE_IMAGE_URL;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.dateFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.priceLabel;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.scheduleFormatter;
import static ar.edu.itba.paw.webapp.utils.ViewFormatUtils.timeFormatter;

import ar.edu.itba.paw.models.EventJoinPolicy;
import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.EventVisibility;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.BookingDetailViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventDetailPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.EventOccurrenceViewModel;
import ar.edu.itba.paw.webapp.viewmodel.UiViewModels.ParticipantViewModel;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventController {
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final MatchService matchService;
    private final MatchReservationService matchReservationService;
    private final MatchParticipationService matchParticipationService;
    private final PlayerReviewService playerReviewService;
    private final UserService userService;
    private final MessageSource messageSource;
    private final Clock clock;
    private final boolean mapPickerEnabled;
    private final String mapTileUrlTemplate;
    private final String mapAttribution;
    private final int mapDefaultZoom;

    public EventController(
            final MatchService matchService,
            final MatchReservationService matchReservationService,
            final MatchParticipationService matchParticipationService,
            final PlayerReviewService playerReviewService,
            final UserService userService,
            final MessageSource messageSource,
            final Clock clock) {
        this(
                matchService,
                matchReservationService,
                matchParticipationService,
                playerReviewService,
                userService,
                messageSource,
                clock,
                false,
                "",
                "",
                DEFAULT_MAP_ZOOM);
    }

    @Autowired
    public EventController(
            final MatchService matchService,
            final MatchReservationService matchReservationService,
            final MatchParticipationService matchParticipationService,
            final PlayerReviewService playerReviewService,
            final UserService userService,
            final MessageSource messageSource,
            final Clock clock,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.matchService = matchService;
        this.matchReservationService = matchReservationService;
        this.matchParticipationService = matchParticipationService;
        this.playerReviewService = playerReviewService;
        this.userService = userService;
        this.messageSource = messageSource;
        this.clock = clock;
        this.mapPickerEnabled = mapPickerEnabled;
        this.mapTileUrlTemplate = mapTileUrlTemplate == null ? "" : mapTileUrlTemplate;
        this.mapAttribution = mapAttribution == null ? "" : mapAttribution;
        this.mapDefaultZoom = mapDefaultZoom;
    }

    @GetMapping("/matches/{eventId}")
    public ModelAndView showEventDetails(
            @PathVariable("eventId") final String eventId,
            @RequestParam(value = "reservation", required = false) final String reservationStatus,
            @RequestParam(value = "reservationError", required = false)
                    final String reservationError,
            @RequestParam(value = "seriesReservationError", required = false)
                    final String seriesReservationErrorCode,
            @RequestParam(value = "hostAction", required = false) final String hostAction,
            @RequestParam(value = "join", required = false) final String joinStatus,
            @RequestParam(value = "joinError", required = false) final String joinErrorCode,
            @RequestParam(value = "invite", required = false) final String inviteStatus,
            @RequestParam(value = "inviteError", required = false) final String inviteErrorCode,
            final Model model,
            final Locale locale) {
        final String resolvedReservationStatus =
                flashString(model, "reservationStatus").orElse(reservationStatus);
        final String resolvedJoinStatus = flashString(model, "joinStatus").orElse(joinStatus);
        final String resolvedInviteStatus = flashString(model, "inviteStatus").orElse(inviteStatus);
        return showRealEventDetails(
                parseEventIdOrThrowNotFound(eventId),
                resolvedReservationStatus,
                flashString(model, "hostAction").orElse(hostAction),
                reservationError,
                seriesReservationErrorCode == null
                        ? null
                        : reservationErrorMessage(seriesReservationErrorCode, locale),
                resolvedJoinStatus,
                Boolean.TRUE.equals(model.asMap().get("joinRequested")),
                Boolean.TRUE.equals(model.asMap().get("seriesJoinRequested")),
                joinErrorCode == null ? null : joinErrorMessage(joinErrorCode, locale),
                resolvedInviteStatus,
                inviteErrorCode == null ? null : inviteErrorMessage(inviteErrorCode, locale),
                locale);
    }

    @PostMapping("/matches/{eventId}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("eventId") final String eventId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            matchReservationService.reserveSpot(matchId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("reservationStatus", "confirmed");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return showRealEventDetails(
                    matchId,
                    null,
                    null,
                    reservationErrorMessage(exception.getCode(), locale),
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    locale);
        }
    }

    @PostMapping("/matches/{eventId}/reservations/cancel")
    public ModelAndView cancelReservation(
            @PathVariable("eventId") final String eventId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        final Match cancellationContext = matchService.findMatchById(matchId).orElse(null);

        try {
            matchParticipationService.removeParticipant(
                    matchId, currentUser.getUserId(), currentUser.getUserId());
            if (shouldRedirectToPlayerMatchesAfterCancellation(
                    cancellationContext, currentUser.getUserId())) {
                return new ModelAndView("redirect:/events");
            }
            redirectAttributes.addFlashAttribute("reservationStatus", "cancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException exception) {
            return showRealEventDetails(
                    matchId,
                    null,
                    null,
                    reservationErrorMessage(exception.getCode(), locale),
                    null,
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    locale);
        }
    }

    @PostMapping({
        "/matches/{eventId}/recurring-reservations",
        "/matches/{eventId}/series-reservations"
    })
    public ModelAndView requestSeriesReservation(
            @PathVariable("eventId") final String eventId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            matchReservationService.reserveSeries(matchId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("reservationStatus", "recurringConfirmed");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return showRealEventDetails(
                    matchId,
                    null,
                    null,
                    null,
                    reservationErrorMessage(exception.getCode(), locale),
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    locale);
        }
    }

    @PostMapping({
        "/matches/{eventId}/recurring-reservations/cancel",
        "/matches/{eventId}/series-reservations/cancel"
    })
    public ModelAndView cancelSeriesReservations(
            @PathVariable("eventId") final String eventId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            matchReservationService.cancelSeriesReservations(matchId, currentUser.getUserId());
            redirectAttributes.addFlashAttribute("reservationStatus", "recurringCancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return showRealEventDetails(
                    matchId,
                    null,
                    null,
                    null,
                    reservationErrorMessage(exception.getCode(), locale),
                    null,
                    false,
                    false,
                    null,
                    null,
                    null,
                    locale);
        }
    }

    private ModelAndView showRealEventDetails(
            final Long eventId,
            final String reservationStatus,
            final String hostAction,
            final String reservationError,
            final String seriesReservationError,
            final String joinStatus,
            final boolean joinRequestedFlash,
            final boolean seriesJoinRequestedFlash,
            final String joinError,
            final String inviteStatus,
            final String inviteError,
            final Locale locale) {
        final Match match =
                matchService
                        .findMatchById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        final Long currentUserId =
                CurrentAuthenticatedUser.get()
                        .map(AuthenticatedUserPrincipal::getUserId)
                        .orElse(null);

        if (!isMatchVisibleToUser(match, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final boolean isHostViewer =
                currentUserId != null && currentUserId.equals(match.getHostUserId());
        final boolean hasPendingRequest =
                !isHostViewer
                        && currentUserId != null
                        && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                        && matchParticipationService.hasPendingRequest(eventId, currentUserId);
        final boolean isInvitedPlayer =
                !isHostViewer
                        && currentUserId != null
                        && match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY
                        && matchParticipationService.hasInvitation(eventId, currentUserId);
        final boolean isConfirmedParticipant =
                currentUserId != null
                        && matchReservationService.hasActiveReservation(
                                match.getId(), currentUserId);
        final boolean isApprovalRequired =
                match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED;
        final boolean isInviteOnly = match.getJoinPolicy() == EventJoinPolicy.INVITE_ONLY;
        final boolean isPrivateEvent = match.getVisibility() == EventVisibility.PRIVATE;

        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final List<Match> seriesOccurrences =
                match.isRecurringOccurrence()
                        ? matchService.findSeriesOccurrences(match.getSeriesId())
                        : List.of();
        final SeriesReservationUiState seriesReservationState =
                buildSeriesReservationUiState(
                        match.getSeriesId(), seriesOccurrences, currentUserId, isHostViewer);
        final SeriesJoinRequestUiState seriesJoinRequestState =
                isHostViewer
                        ? new SeriesJoinRequestUiState(false, false)
                        : buildSeriesJoinRequestUiState(match, seriesOccurrences, currentUserId);
        final boolean suppressReservationErrors =
                hasPendingRequest || seriesJoinRequestState.pending();
        final ModelAndView mav = new ModelAndView("matches/detail");
        final boolean hostCanManage = isHost(match, currentUserId);
        mav.addObject("isConfirmedParticipant", isConfirmedParticipant);
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("isInviteOnly", isInviteOnly);
        mav.addObject("reservationRequiresLogin", CurrentAuthenticatedUser.get().isEmpty());
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject(
                "eventPage",
                buildRealEventPage(
                        match, confirmedParticipants, seriesOccurrences, currentUserId, locale));

        mav.addObject("reservationEnabled", canReserveMatch(match, isHostViewer));
        mav.addObject("reservationRequestPath", "/matches/" + eventId + "/reservations");
        mav.addObject("reservationCancelPath", "/matches/" + eventId + "/reservations/cancel");
        mav.addObject(
                "reservationCancellationEnabled",
                isConfirmedParticipant && canCancelReservation(match));
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
        mav.addObject("seriesReservationRequiresLogin", currentUserId == null);
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

        mav.addObject(
                "joinRequestEnabled",
                !isHostViewer && canRequestToJoin(match) && !seriesJoinRequestState.pending());
        mav.addObject("joinRequestPath", "/matches/" + eventId + "/join-requests");
        mav.addObject("seriesJoinRequestPath", "/matches/" + eventId + "/recurring-join-requests");
        mav.addObject("seriesJoinRequestEnabled", seriesJoinRequestState.available());
        mav.addObject("seriesJoinRequestPending", seriesJoinRequestState.pending());
        mav.addObject("seriesJoinRequestRequiresLogin", currentUserId == null);
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

        mav.addObject("hostViewer", isHostViewer);
        mav.addObject("isPrivateEvent", isPrivateEvent);
        mav.addObject("hostRequestsPath", "/host/matches/" + eventId + "/requests");
        mav.addObject("hostInvitesPath", "/host/matches/" + eventId + "/invites");
        mav.addObject("hostParticipantsPath", "/host/matches/" + eventId + "/participants");
        mav.addObject("hostCanManage", hostCanManage);
        mav.addObject(
                "hostCanManageParticipants", hostCanManage && canHostManageParticipants(match));
        mav.addObject("hostCanEdit", hostCanManage && canHostEdit(match));
        mav.addObject("hostCanCancel", hostCanManage && canHostCancel(match));
        mav.addObject(
                "hostCanEditSeries",
                hostCanManage && match.isRecurringOccurrence() && canHostEdit(match));
        mav.addObject(
                "hostCanCancelSeries",
                hostCanManage && match.isRecurringOccurrence() && canHostCancel(match));
        mav.addObject("hostEditPath", "/host/matches/" + eventId + "/edit");
        mav.addObject("hostCancelPath", "/host/matches/" + eventId + "/cancel");
        mav.addObject("hostSeriesEditPath", "/host/matches/" + eventId + "/series/edit");
        mav.addObject("hostSeriesCancelPath", "/host/matches/" + eventId + "/series/cancel");
        mav.addObject("hostActionNotice", hostActionNotice(hostAction, locale));
        return mav;
    }

    private EventDetailPageViewModel buildRealEventPage(
            final Match match,
            final List<User> confirmedParticipants,
            final List<Match> seriesOccurrences,
            final Long currentUserId,
            final Locale locale) {
        final Optional<User> host = userService.findById(match.getHostUserId());
        final Set<Long> reviewableUserIds =
                currentUserId == null
                        ? Set.of()
                        : Optional.ofNullable(
                                        playerReviewService.findReviewableUserIds(currentUserId))
                                .orElseGet(Set::of);
        return new EventDetailPageViewModel(
                toCard(
                        match,
                        ZoneId.systemDefault(),
                        locale,
                        currentUserId,
                        buildAvailabilityLabel(match, locale),
                        messageSource,
                        userService,
                        matchParticipationService,
                        matchReservationService),
                null,
                null,
                host.map(User::getUsername)
                        .orElse(
                                messageSource.getMessage(
                                        "event.detail.unknownHost",
                                        new Object[] {match.getHostUserId()},
                                        locale)),
                host.map(this::profileHrefFor).orElse(null),
                host.map(user -> profileUrlFor(user)).orElse(DEFAULT_PROFILE_IMAGE_URL),
                toParticipantViewModels(confirmedParticipants, currentUserId, reviewableUserIds),
                buildParticipantCountLabel(confirmedParticipants.size(), locale),
                messageSource.getMessage("event.detail.noPlayersHint", null, locale),
                buildAboutParagraphs(match, locale),
                priceLabel(match.getPricePerPlayer(), locale, messageSource),
                buildBookingDetails(match, locale),
                buildAvailabilityLabel(match, locale),
                messageSource.getMessage("event.booking.cta", null, locale),
                loadNearbyMatches(match.getId(), currentUserId, locale),
                toOccurrenceViewModels(match, seriesOccurrences, currentUserId, locale),
                mapPickerEnabled && !mapTileUrlTemplate.isBlank() && match.hasCoordinates(),
                match.getLatitude(),
                match.getLongitude(),
                mapTileUrlTemplate,
                mapAttribution,
                mapDefaultZoom);
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
            final Long currentUserId,
            final Set<Long> reviewableUserIds) {
        return confirmedParticipants.stream()
                .map(
                        participant ->
                                new ParticipantViewModel(
                                        participant.getUsername(),
                                        avatarLabelForUsername(participant.getUsername()),
                                        profileHrefFor(participant),
                                        profileImageUrlForParticipant(participant),
                                        reviewHrefForParticipant(
                                                participant, currentUserId, reviewableUserIds)))
                .toList();
    }

    private String reviewHrefForParticipant(
            final User participant, final Long currentUserId, final Set<Long> reviewableUserIds) {
        if (currentUserId == null
                || participant.getId() == null
                || currentUserId.equals(participant.getId())
                || !reviewableUserIds.contains(participant.getId())) {
            return null;
        }
        return profileHrefFor(participant) + "?reviewForm=open#reviews";
    }

    private String profileImageUrlForParticipant(final User participant) {
        return profileUrlFor(participant);
    }

    private String profileHrefFor(final User user) {
        return "/users/" + user.getUsername();
    }

    private List<EventCardViewModel> loadNearbyMatches(
            final Long currentMatchId, final Long currentUserId, final Locale locale) {
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "", null, null, null, "soonest", 1, 4, null, null, null);
        return result.getItems().stream()
                .filter(match -> !currentMatchId.equals(match.getId()))
                .limit(3)
                .map(
                        match ->
                                toCard(
                                        match,
                                        ZoneId.systemDefault(),
                                        locale,
                                        currentUserId,
                                        buildAvailabilityLabel(match, locale),
                                        messageSource,
                                        userService,
                                        matchParticipationService,
                                        matchReservationService))
                .toList();
    }

    private List<EventOccurrenceViewModel> toOccurrenceViewModels(
            final Match currentMatch,
            final List<Match> occurrences,
            final Long currentUserId,
            final Locale locale) {
        if (occurrences == null || occurrences.size() <= 1) {
            return List.of();
        }

        return occurrences.stream()
                .map(
                        occurrence -> {
                            final EventDisplayState state = eventDisplayState(occurrence);
                            final String href =
                                    isMatchVisibleToUser(occurrence, currentUserId)
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
                                    occurrence.getId().equals(currentMatch.getId()));
                        })
                .toList();
    }

    private SeriesReservationUiState buildSeriesReservationUiState(
            final Long seriesId,
            final List<Match> occurrences,
            final Long currentUserId,
            final boolean isHostViewer) {
        if (occurrences == null || occurrences.isEmpty()) {
            return new SeriesReservationUiState(false, false, false);
        }
        final Set<Long> activeFutureReservationMatchIds =
                currentUserId == null || seriesId == null
                        ? Set.of()
                        : matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                seriesId, currentUserId);
        final SeriesReservationEvaluation evaluation =
                evaluateSeriesReservationTargets(
                        occurrences, currentUserId, activeFutureReservationMatchIds, isHostViewer);
        return new SeriesReservationUiState(
                !evaluation.targetMatchIds().isEmpty(),
                evaluation.joined(),
                evaluation.activeFutureReservationCount() > 0);
    }

    private SeriesReservationEvaluation evaluateSeriesReservationTargets(
            final List<Match> occurrences,
            final Long userId,
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
                userId != null
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
            final Match match, final List<Match> occurrences, final Long currentUserId) {
        if (currentUserId != null
                && match.isRecurringOccurrence()
                && matchParticipationService.hasPendingSeriesRequest(
                        match.getId(), currentUserId)) {
            return new SeriesJoinRequestUiState(false, true);
        }

        final Set<Long> activeFutureReservationMatchIds =
                currentUserId == null || match.getSeriesId() == null
                        ? Set.of()
                        : matchReservationService.findActiveFutureReservationMatchIdsForSeries(
                                match.getSeriesId(), currentUserId);
        final Set<Long> pendingFutureRequestMatchIds =
                currentUserId == null || match.getSeriesId() == null
                        ? Set.of()
                        : matchParticipationService.findPendingFutureRequestMatchIdsForSeries(
                                match.getSeriesId(), currentUserId);
        final SeriesJoinRequestEvaluation evaluation =
                evaluateSeriesJoinRequestTargets(
                        occurrences,
                        currentUserId,
                        activeFutureReservationMatchIds,
                        pendingFutureRequestMatchIds);
        return new SeriesJoinRequestUiState(
                !evaluation.targetMatchIds().isEmpty(), evaluation.pending());
    }

    private SeriesJoinRequestEvaluation evaluateSeriesJoinRequestTargets(
            final List<Match> occurrences,
            final Long userId,
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
                userId != null
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
        if (status.filter(EventStatus.DRAFT::equals).isPresent()) {
            return new EventDisplayState("draft", "draft");
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

    private boolean hasEventStarted(final Match match) {
        return !match.getStartsAt().isAfter(Instant.now(clock));
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

    private static Long parseEventIdOrThrowNotFound(final String eventId) {
        if (eventId == null || !eventId.matches("\\d+")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        try {
            return Long.valueOf(eventId);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    private boolean isMatchVisibleToUser(final Match match, final Long currentUserId) {
        if (EventStatus.DRAFT == match.getStatus()) {
            return currentUserId != null && currentUserId.equals(match.getHostUserId());
        }

        if (match.getVisibility() == EventVisibility.PRIVATE
                || EventStatus.CANCELLED == match.getStatus()) {
            if (currentUserId != null && currentUserId.equals(match.getHostUserId())) {
                return true;
            }
            if (currentUserId != null
                    && matchReservationService.hasActiveReservation(match.getId(), currentUserId)) {
                return true;
            }
            if (currentUserId != null
                    && matchParticipationService.hasInvitation(match.getId(), currentUserId)) {
                return true;
            }
            return false;
        }

        return match.getVisibility() == EventVisibility.PUBLIC;
    }

    private boolean canReserveMatch(final Match match, final boolean isHostViewer) {
        return EventStatus.OPEN == match.getStatus()
                && (isHostViewer
                        || (match.getVisibility() == EventVisibility.PUBLIC
                                && match.getJoinPolicy() == EventJoinPolicy.DIRECT))
                && !hasEventStarted(match)
                && match.getAvailableSpots() > 0;
    }

    private boolean canRequestToJoin(final Match match) {
        return match.getVisibility() == EventVisibility.PUBLIC
                && match.getJoinPolicy() == EventJoinPolicy.APPROVAL_REQUIRED
                && EventStatus.OPEN == match.getStatus()
                && !hasEventStarted(match)
                && match.getAvailableSpots() > 0;
    }

    private boolean canCancelReservation(final Match match) {
        return EventStatus.OPEN == match.getStatus() && !hasEventStarted(match);
    }

    private boolean shouldRedirectToPlayerMatchesAfterCancellation(
            final Match match, final Long userId) {
        return match != null
                && match.getVisibility() == EventVisibility.PRIVATE
                && !userId.equals(match.getHostUserId());
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

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
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
        return null;
    }

    private boolean isHost(final Match match, final Long currentUserId) {
        return currentUserId != null && currentUserId.equals(match.getHostUserId());
    }

    private boolean canHostEdit(final Match match) {
        if (hasEventEnded(match)) {
            return false;
        }
        return match.getStatus() != EventStatus.COMPLETED
                && match.getStatus() != EventStatus.CANCELLED;
    }

    private boolean canHostCancel(final Match match) {
        if (hasEventEnded(match)) {
            return false;
        }
        return match.getStatus() != EventStatus.COMPLETED
                && match.getStatus() != EventStatus.CANCELLED;
    }

    private boolean canHostManageParticipants(final Match match) {
        return canHostEdit(match);
    }

    private record SeriesReservationUiState(
            boolean available, boolean joined, boolean cancellable) {}

    private record SeriesReservationEvaluation(
            List<Long> targetMatchIds, boolean joined, int activeFutureReservationCount) {}

    private record SeriesJoinRequestUiState(boolean available, boolean pending) {}

    private record SeriesJoinRequestEvaluation(List<Long> targetMatchIds, boolean pending) {}

    private record EventDisplayState(String key, String tone) {}
}
