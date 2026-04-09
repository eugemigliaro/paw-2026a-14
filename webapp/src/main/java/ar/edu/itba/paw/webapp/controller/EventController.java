package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.ImageUrlHelper.bannerUrlFor;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
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
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class EventController {

    private static final DateTimeFormatter SCHEDULE_FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(Locale.US);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.US);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.US);

    private final MatchService matchService;
    private final UserService userService;
    private final ActionVerificationService actionVerificationService;

    @Autowired
    public EventController(
            final MatchService matchService,
            final UserService userService,
            final ActionVerificationService actionVerificationService) {
        this.matchService = matchService;
        this.userService = userService;
        this.actionVerificationService = actionVerificationService;
    }

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm() {
        return new ReservationRequestForm();
    }

    @GetMapping("/events/{eventId}")
    public ModelAndView showEventDetails(
            @PathVariable("eventId") final String eventId,
            @RequestParam(value = "reservation", required = false) final String reservationStatus) {
        return showRealEventDetails(
                parseEventIdOrThrowNotFound(eventId), reservationStatus, null, null);
    }

    @PostMapping("/events/{eventId}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("eventId") final String eventId,
            @Valid @ModelAttribute("reservationRequestForm")
                    final ReservationRequestForm reservationRequestForm,
            final BindingResult bindingResult) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        if (bindingResult.hasErrors()) {
            return showRealEventDetails(
                    matchId,
                    null,
                    bindingResult.getFieldError("email") == null
                            ? "Enter a valid email address."
                            : bindingResult.getFieldError("email").getDefaultMessage(),
                    reservationRequestForm);
        }

        try {
            final VerificationRequestResult requestResult =
                    actionVerificationService.requestMatchReservation(
                            matchId, reservationRequestForm.getEmail());
            final Match match =
                    matchService
                            .findPublicMatchById(matchId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            final ModelAndView mav = new ModelAndView("verification/check-email");
            mav.addObject("shell", ShellViewModelFactory.browseShell());
            mav.addObject("title", "Check your email");
            mav.addObject(
                    "summary",
                    "We sent a one-time confirmation link to "
                            + requestResult.getEmail()
                            + " so you can reserve your spot in "
                            + match.getTitle()
                            + ".");
            mav.addObject(
                    "expiresAtLabel",
                    SCHEDULE_FORMATTER.format(
                            requestResult.getExpiresAt().atZone(ZoneId.systemDefault())));
            mav.addObject("backHref", "/events/" + matchId);
            return mav;
        } catch (final VerificationFailureException exception) {
            return showRealEventDetails(
                    matchId, null, exception.getMessage(), reservationRequestForm);
        }
    }

    private ModelAndView showRealEventDetails(
            final Long eventId,
            final String reservationStatus,
            final String reservationError,
            final ReservationRequestForm reservationRequestForm) {
        final Match match =
                matchService
                        .findPublicMatchById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final ModelAndView mav = new ModelAndView("events/detail");
        mav.addObject("shell", ShellViewModelFactory.browseShell());
        mav.addObject("eventPage", buildRealEventPage(match, confirmedParticipants));
        mav.addObject("reservationEnabled", match.getAvailableSpots() > 0);
        mav.addObject("reservationRequestPath", "/events/" + eventId + "/reservations");
        mav.addObject("reservationError", reservationError);
        mav.addObject("reservationConfirmed", "confirmed".equalsIgnoreCase(reservationStatus));
        if (reservationRequestForm != null) {
            mav.addObject("reservationRequestForm", reservationRequestForm);
        }
        return mav;
    }

    private EventDetailPageViewModel buildRealEventPage(
            final Match match, final List<User> confirmedParticipants) {
        return new EventDetailPageViewModel(
                toCard(match),
                null,
                null,
                userService
                        .findById(match.getHostUserId())
                        .map(user -> user.getUsername())
                        .orElse("Host #" + match.getHostUserId()),
                toParticipantViewModels(confirmedParticipants),
                buildParticipantCountLabel(confirmedParticipants.size()),
                "No one has joined yet. Be the first confirmed player.",
                buildAboutParagraphs(match),
                toPriceLabel(match.getPricePerPlayer()),
                buildBookingDetails(match),
                buildAvailabilityLabel(match),
                "Reserve a spot",
                loadNearbyEvents(match.getId()));
    }

    private List<String> buildAboutParagraphs(final Match match) {
        final String description =
                match.getDescription() == null || match.getDescription().isBlank()
                        ? "A community sports event hosted through Match Point."
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

    private List<BookingDetailViewModel> buildBookingDetails(final Match match) {
        return List.of(
                new BookingDetailViewModel(
                        "Date",
                        DATE_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault()))),
                new BookingDetailViewModel(
                        "Time",
                        TIME_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault()))
                                + (match.getEndsAt() == null
                                        ? ""
                                        : " - "
                                                + TIME_FORMATTER.format(
                                                        match.getEndsAt()
                                                                .atZone(ZoneId.systemDefault())))),
                new BookingDetailViewModel("Venue", match.getAddress()));
    }

    private List<ParticipantViewModel> toParticipantViewModels(
            final List<User> confirmedParticipants) {
        return confirmedParticipants.stream()
                .map(
                        participant ->
                                new ParticipantViewModel(
                                        participant.getUsername(),
                                        avatarLabelForUsername(participant.getUsername())))
                .toList();
    }

    private List<EventCardViewModel> loadNearbyEvents(final Long currentMatchId) {
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches("", null, "all", "soonest", 1, 4, null);
        return result.getItems().stream()
                .filter(match -> !currentMatchId.equals(match.getId()))
                .limit(3)
                .map(this::toCard)
                .toList();
    }

    private EventCardViewModel toCard(final Match match) {
        return new EventCardViewModel(
                String.valueOf(match.getId()),
                "/events/" + match.getId(),
                match.getSport().getDisplayName(),
                match.getTitle(),
                match.getAddress(),
                SCHEDULE_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault())),
                toPriceLabel(match.getPricePerPlayer()),
                buildAvailabilityLabel(match),
                null,
                mediaClassFor(match.getSport()),
                bannerUrlFor(match));
    }

    private static String buildAvailabilityLabel(final Match match) {
        return match.getAvailableSpots() + " of " + match.getMaxPlayers() + " spots left";
    }

    private static String buildParticipantCountLabel(final int participantCount) {
        return participantCount == 1
                ? "1 confirmed player"
                : participantCount + " confirmed players";
    }

    private static String toPriceLabel(final BigDecimal pricePerPlayer) {
        if (pricePerPlayer == null) {
            return "Price TBD";
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0 ? "Free" : "$" + pricePerPlayer;
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
}
