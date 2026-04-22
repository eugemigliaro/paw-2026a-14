package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.DEFAULT_PROFILE_IMAGE_URL;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;
import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.profileUrlFor;

import ar.edu.itba.paw.models.EventStatus;
import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.webapp.security.AuthenticatedUserPrincipal;
import ar.edu.itba.paw.webapp.security.CurrentAuthenticatedUser;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.BookingDetailViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventDetailPageViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.ParticipantViewModel;
import ar.edu.itba.paw.webapp.viewmodel.ShellViewModelFactory;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class EventController {

    private final MatchService matchService;
    private final MatchReservationService matchReservationService;
    private final MatchParticipationService matchParticipationService;
    private final UserService userService;
    private final MessageSource messageSource;

    @Autowired
    public EventController(
            final MatchService matchService,
            final MatchReservationService matchReservationService,
            final MatchParticipationService matchParticipationService,
            final UserService userService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.matchReservationService = matchReservationService;
        this.matchParticipationService = matchParticipationService;
        this.userService = userService;
        this.messageSource = messageSource;
    }

    @GetMapping("/matches/{eventId}")
    public ModelAndView showEventDetails(
            @PathVariable("eventId") final String eventId,
            @RequestParam(value = "reservation", required = false) final String reservationStatus,
            @RequestParam(value = "reservationError", required = false)
                    final String reservationError,
            @RequestParam(value = "hostAction", required = false) final String hostAction,
            @RequestParam(value = "join", required = false) final String joinStatus,
            @RequestParam(value = "joinError", required = false) final String joinErrorCode,
            @RequestParam(value = "invite", required = false) final String inviteStatus,
            @RequestParam(value = "inviteError", required = false) final String inviteErrorCode,
            final Locale locale) {
        return showRealEventDetails(
                parseEventIdOrThrowNotFound(eventId),
                reservationStatus,
                hostAction,
                reservationError,
                joinStatus,
                joinErrorCode == null ? null : joinErrorMessage(joinErrorCode, locale),
                inviteStatus,
                inviteErrorCode == null ? null : inviteErrorMessage(inviteErrorCode, locale),
                locale);
    }

    @PostMapping("/matches/{eventId}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("eventId") final String eventId, final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        final AuthenticatedUserPrincipal currentUser =
                CurrentAuthenticatedUser.get()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        try {
            matchReservationService.reserveSpot(matchId, currentUser.getUserId());
            return new ModelAndView("redirect:/matches/" + matchId + "?reservation=confirmed");
        } catch (final MatchReservationException exception) {
            return showRealEventDetails(
                    matchId,
                    null,
                    null,
                    reservationErrorMessage(exception.getCode(), locale),
                    null,
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
            final String joinStatus,
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

        final boolean hasPendingRequest =
                currentUserId != null
                        && "approval_required".equalsIgnoreCase(match.getJoinPolicy())
                        && matchParticipationService.hasPendingRequest(eventId, currentUserId);
        final boolean isInvitedPlayer =
                currentUserId != null
                        && "invite_only".equalsIgnoreCase(match.getJoinPolicy())
                        && matchParticipationService.hasInvitation(eventId, currentUserId);
        final boolean isConfirmedParticipant =
                currentUserId != null
                        && matchReservationService.hasActiveReservation(
                                match.getId(), currentUserId);
        final boolean isApprovalRequired =
                "approval_required".equalsIgnoreCase(match.getJoinPolicy());
        final boolean isInviteOnly = "invite_only".equalsIgnoreCase(match.getJoinPolicy());
        final boolean isHostViewer =
                currentUserId != null && currentUserId.equals(match.getHostUserId());
        final boolean isPrivateEvent = "private".equalsIgnoreCase(match.getVisibility());

        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final ModelAndView mav = new ModelAndView("matches/detail");
        final boolean hostCanManage = isHost(match, currentUserId);
        mav.addObject("isConfirmedParticipant", isConfirmedParticipant);
        mav.addObject("isApprovalRequired", isApprovalRequired);
        mav.addObject("isInviteOnly", isInviteOnly);
        mav.addObject("reservationRequiresLogin", CurrentAuthenticatedUser.get().isEmpty());
        mav.addObject("shell", ShellViewModelFactory.playerShell(messageSource, locale));
        mav.addObject("eventPage", buildRealEventPage(match, confirmedParticipants, locale));

        mav.addObject("reservationEnabled", canReserveMatch(match));
        mav.addObject("reservationRequestPath", "/matches/" + eventId + "/reservations");
        mav.addObject("reservationError", reservationError);
        mav.addObject("reservationConfirmed", "confirmed".equalsIgnoreCase(reservationStatus));

        mav.addObject("joinRequestEnabled", canRequestToJoin(match));
        mav.addObject("joinRequestPath", "/matches/" + eventId + "/join-requests");
        mav.addObject("cancelJoinRequestPath", "/matches/" + eventId + "/join-requests/cancel");
        mav.addObject("hasPendingJoinRequest", hasPendingRequest);
        mav.addObject("joinRequested", "requested".equalsIgnoreCase(joinStatus));
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
        mav.addObject("hostEditPath", "/host/matches/" + eventId + "/edit");
        mav.addObject("hostCancelPath", "/host/matches/" + eventId + "/cancel");
        mav.addObject("hostActionNotice", hostActionNotice(hostAction, locale));
        return mav;
    }

    private EventDetailPageViewModel buildRealEventPage(
            final Match match, final List<User> confirmedParticipants, final Locale locale) {
        final Optional<User> host = userService.findById(match.getHostUserId());
        return new EventDetailPageViewModel(
                toCard(match, locale),
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
                toParticipantViewModels(confirmedParticipants),
                buildParticipantCountLabel(confirmedParticipants.size(), locale),
                messageSource.getMessage("event.detail.noPlayersHint", null, locale),
                buildAboutParagraphs(match, locale),
                toPriceLabel(match.getPricePerPlayer(), locale),
                buildBookingDetails(match, locale),
                buildAvailabilityLabel(match, locale),
                messageSource.getMessage("event.booking.cta", null, locale),
                loadNearbyMatches(match.getId(), locale));
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
            final List<User> confirmedParticipants) {
        return confirmedParticipants.stream()
                .map(
                        participant ->
                                new ParticipantViewModel(
                                        participant.getUsername(),
                                        avatarLabelForUsername(participant.getUsername()),
                                        profileHrefFor(participant),
                                        profileImageUrlForParticipant(participant)))
                .toList();
    }

    private String profileImageUrlForParticipant(final User participant) {
        return profileUrlFor(participant);
    }

    private String profileHrefFor(final User user) {
        return "/users/" + user.getUsername();
    }

    private List<EventCardViewModel> loadNearbyMatches(
            final Long currentMatchId, final Locale locale) {
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "", null, null, null, "soonest", 1, 4, null, null, null);
        return result.getItems().stream()
                .filter(match -> !currentMatchId.equals(match.getId()))
                .limit(3)
                .map(match -> toCard(match, locale))
                .toList();
    }

    private EventCardViewModel toCard(final Match match, final Locale locale) {
        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/matches/" + match.getId(),
                toSportLabel(match.getSport(), locale),
                match.getTitle(),
                match.getAddress(),
                scheduleFormatter(locale)
                        .format(match.getStartsAt().atZone(ZoneId.systemDefault())),
                toPriceLabel(match.getPricePerPlayer(), locale),
                buildAvailabilityLabel(match, locale),
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
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

    private String toPriceLabel(final BigDecimal pricePerPlayer, final Locale locale) {
        if (pricePerPlayer == null) {
            return messageSource.getMessage("price.tbd", null, locale);
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0
                ? messageSource.getMessage("price.free", null, locale)
                : messageSource.getMessage("price.amount", new Object[] {pricePerPlayer}, locale);
    }

    private String toSportLabel(final Sport sport, final Locale locale) {
        return messageSource.getMessage(
                "sport." + sport.getDbValue(),
                null,
                sport.getDisplayName(),
                resolvedLocale(locale));
    }

    private static DateTimeFormatter scheduleFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    private String reservationErrorMessage(final String code, final Locale locale) {
        switch (code) {
            case "closed":
                return messageSource.getMessage("reservation.error.closed", null, locale);
            case "started":
                return messageSource.getMessage("reservation.error.started", null, locale);
            case "already_joined":
                return messageSource.getMessage("reservation.error.alreadyJoined", null, locale);
            case "full":
                return messageSource.getMessage(
                        "reservation.error.fullBeforeConfirm", null, locale);
            case "not_found":
            default:
                return messageSource.getMessage("reservation.error.notFound", null, locale);
        }
    }

    private static DateTimeFormatter dateFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(resolvedLocale(locale));
    }

    private static DateTimeFormatter timeFormatter(final Locale locale) {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(resolvedLocale(locale));
    }

    private static Locale resolvedLocale(final Locale locale) {
        return locale == null ? Locale.ENGLISH : locale;
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

    private static String mediaClassFor(final Sport sport) {
        switch (sport) {
            case FOOTBALL:
                return "media-tile--football";
            case TENNIS:
                return "media-tile--tennis";
            case BASKETBALL:
                return "media-tile--basketball";
            case PADEL:
            default:
                return "media-tile--padel";
        }
    }

    private boolean isMatchVisibleToUser(final Match match, final Long currentUserId) {
        if ("draft".equalsIgnoreCase(match.getStatus())) {
            return currentUserId != null && currentUserId.equals(match.getHostUserId());
        }

        if ("private".equalsIgnoreCase(match.getVisibility())
                || "cancelled".equalsIgnoreCase(match.getStatus())) {
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

        return "public".equalsIgnoreCase(match.getVisibility());
    }

    private boolean canReserveMatch(final Match match) {
        return "public".equalsIgnoreCase(match.getVisibility())
                && "direct".equalsIgnoreCase(match.getJoinPolicy())
                && "open".equalsIgnoreCase(match.getStatus())
                && match.getAvailableSpots() > 0;
    }

    private boolean canRequestToJoin(final Match match) {
        return "public".equalsIgnoreCase(match.getVisibility())
                && "approval_required".equalsIgnoreCase(match.getJoinPolicy())
                && "open".equalsIgnoreCase(match.getStatus())
                && match.getAvailableSpots() > 0;
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
        return null;
    }

    private boolean isHost(final Match match, final Long currentUserId) {
        return currentUserId != null && currentUserId.equals(match.getHostUserId());
    }

    private boolean canHostEdit(final Match match) {
        return EventStatus.fromDbValue(match.getStatus())
                .map(status -> status != EventStatus.COMPLETED && status != EventStatus.CANCELLED)
                .orElse(true);
    }

    private boolean canHostCancel(final Match match) {
        return EventStatus.fromDbValue(match.getStatus())
                .map(status -> status != EventStatus.COMPLETED && status != EventStatus.CANCELLED)
                .orElse(true);
    }

    private boolean canHostManageParticipants(final Match match) {
        return canHostEdit(match);
    }
}
