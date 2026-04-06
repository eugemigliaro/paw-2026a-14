package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.PaginatedResult;
import ar.edu.itba.paw.models.Sport;
import ar.edu.itba.paw.services.ActionVerificationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.VerificationFailureException;
import ar.edu.itba.paw.services.VerificationRequestResult;
import ar.edu.itba.paw.webapp.form.ReservationRequestForm;
import ar.edu.itba.paw.webapp.viewmodel.PawUiMockData;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.BookingDetailViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventCardViewModel;
import ar.edu.itba.paw.webapp.viewmodel.PawUiViewModels.EventDetailPageViewModel;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
        if (isNumeric(eventId)) {
            return showRealEventDetails(Long.valueOf(eventId), reservationStatus, null, null);
        }

        return showMockEventDetails(eventId);
    }

    @PostMapping("/events/{eventId}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("eventId") final Long eventId,
            @Valid @ModelAttribute("reservationRequestForm")
            final ReservationRequestForm reservationRequestForm,
            final BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return showRealEventDetails(
                    eventId,
                    null,
                    bindingResult.getFieldError("email") == null
                            ? "Enter a valid email address."
                            : bindingResult.getFieldError("email").getDefaultMessage(),
                    reservationRequestForm);
        }

        try {
            final VerificationRequestResult requestResult =
                    actionVerificationService.requestMatchReservation(
                            eventId, reservationRequestForm.getEmail());
            final Match match =
                    matchService.findPublicMatchById(eventId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            final ModelAndView mav = new ModelAndView("verification/check-email");
            mav.addObject("shell", PawUiMockData.browseShell());
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
            mav.addObject("backHref", "/events/" + eventId);
            return mav;
        } catch (final VerificationFailureException exception) {
            return showRealEventDetails(eventId, null, exception.getMessage(), reservationRequestForm);
        }
    }

    private ModelAndView showMockEventDetails(final String eventId) {
        final Optional<EventDetailPageViewModel> eventPage = PawUiMockData.findEventPage(eventId);

        if (eventPage.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final ModelAndView mav = new ModelAndView("events/detail");
        mav.addObject("shell", PawUiMockData.browseShell());
        mav.addObject("eventPage", eventPage.get());
        mav.addObject("realEvent", false);
        return mav;
    }

    private ModelAndView showRealEventDetails(
            final Long eventId,
            final String reservationStatus,
            final String reservationError,
            final ReservationRequestForm reservationRequestForm) {
        final Match match =
                matchService.findPublicMatchById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final ModelAndView mav = new ModelAndView("events/detail");
        mav.addObject("shell", PawUiMockData.browseShell());
        mav.addObject("eventPage", buildRealEventPage(match));
        mav.addObject("realEvent", true);
        mav.addObject("reservationEnabled", match.getAvailableSpots() > 0);
        mav.addObject("availabilityPercent", calculateAvailabilityPercent(match));
        mav.addObject("reservationRequestPath", "/events/" + eventId + "/reservations");
        mav.addObject("reservationError", reservationError);
        mav.addObject("reservationConfirmed", "confirmed".equalsIgnoreCase(reservationStatus));
        if (reservationRequestForm != null) {
            mav.addObject("reservationRequestForm", reservationRequestForm);
        }
        return mav;
    }

    private EventDetailPageViewModel buildRealEventPage(final Match match) {
        return new EventDetailPageViewModel(
                toCard(match),
                match.getSport().getDisplayName() + " event",
                SCHEDULE_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault())),
                userService
                        .findById(match.getHostUserId())
                        .map(user -> user.getUsername())
                        .orElse("Host #" + match.getHostUserId()),
                buildAboutParagraphs(match),
                List.of(),
                match.getAddress(),
                "Use your email to request a one-time reservation confirmation link.",
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
        return List.of(description);
    }

    private List<BookingDetailViewModel> buildBookingDetails(final Match match) {
        return List.of(
                new BookingDetailViewModel(
                        "Date", DATE_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault()))),
                new BookingDetailViewModel(
                        "Time",
                        TIME_FORMATTER.format(match.getStartsAt().atZone(ZoneId.systemDefault()))
                                + (match.getEndsAt() == null
                                        ? ""
                                        : " - "
                                                + TIME_FORMATTER.format(
                                                        match.getEndsAt()
                                                                .atZone(ZoneId.systemDefault())))),
                new BookingDetailViewModel("Venue", match.getAddress()),
                new BookingDetailViewModel("Availability", buildAvailabilityLabel(match)));
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
                "All levels",
                mediaClassFor(match.getSport()),
                List.of());
    }

    private static String buildAvailabilityLabel(final Match match) {
        return match.getAvailableSpots() + " of " + match.getMaxPlayers() + " spots left";
    }

    private static int calculateAvailabilityPercent(final Match match) {
        if (match.getMaxPlayers() <= 0) {
            return 0;
        }
        final int reserved = Math.max(match.getJoinedPlayers(), 0);
        return Math.min(100, (reserved * 100) / match.getMaxPlayers());
    }

    private static String toPriceLabel(final BigDecimal pricePerPlayer) {
        if (pricePerPlayer == null) {
            return "Price TBD";
        }
        return pricePerPlayer.compareTo(BigDecimal.ZERO) == 0 ? "Free" : "$" + pricePerPlayer;
    }

    private static boolean isNumeric(final String value) {
        return value != null && value.matches("\\d+");
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
