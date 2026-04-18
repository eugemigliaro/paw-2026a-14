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
import org.springframework.context.MessageSource;
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

    private final MatchService matchService;
    private final UserService userService;
    private final ActionVerificationService actionVerificationService;
    private final MessageSource messageSource;

    @Autowired
    public EventController(
            final MatchService matchService,
            final UserService userService,
            final ActionVerificationService actionVerificationService,
            final MessageSource messageSource) {
        this.matchService = matchService;
        this.userService = userService;
        this.actionVerificationService = actionVerificationService;
        this.messageSource = messageSource;
    }

    @ModelAttribute("reservationRequestForm")
    public ReservationRequestForm reservationRequestForm() {
        return new ReservationRequestForm();
    }

    @GetMapping("/matches/{eventId}")
    public ModelAndView showEventDetails(
            @PathVariable("eventId") final String eventId,
            @RequestParam(value = "reservation", required = false) final String reservationStatus,
            final Locale locale) {
        return showRealEventDetails(
                parseEventIdOrThrowNotFound(eventId), reservationStatus, null, null, locale);
    }

    @PostMapping("/matches/{eventId}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("eventId") final String eventId,
            @Valid @ModelAttribute("reservationRequestForm")
                    final ReservationRequestForm reservationRequestForm,
            final BindingResult bindingResult,
            final Locale locale) {
        final Long matchId = parseEventIdOrThrowNotFound(eventId);
        if (bindingResult.hasErrors()) {
            return showRealEventDetails(
                    matchId,
                    null,
                    bindingResult.getFieldError("email") == null
                            ? messageSource.getMessage("form.email.invalid", null, locale)
                            : bindingResult.getFieldError("email").getDefaultMessage(),
                    reservationRequestForm,
                    locale);
        }

        try {
            final VerificationRequestResult requestResult =
                    actionVerificationService.requestMatchReservation(
                            matchId, reservationRequestForm.getEmail());
            final Match match =
                    matchService
                            .findMatchById(matchId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            final ModelAndView mav = new ModelAndView("verification/check-email");
            mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
            mav.addObject(
                    "title", messageSource.getMessage("verification.checkEmail", null, locale));
            mav.addObject(
                    "summary",
                    messageSource.getMessage(
                            "verification.reservation.summary",
                            new Object[] {requestResult.getEmail(), match.getTitle()},
                            locale));
            mav.addObject(
                    "expiresAtLabel",
                    scheduleFormatter(locale)
                            .format(requestResult.getExpiresAt().atZone(ZoneId.systemDefault())));
            mav.addObject("backHref", "/matches/" + matchId);
            return mav;
        } catch (final VerificationFailureException exception) {
            return showRealEventDetails(
                    matchId, null, exception.getMessage(), reservationRequestForm, locale);
        }
    }

    private ModelAndView showRealEventDetails(
            final Long eventId,
            final String reservationStatus,
            final String reservationError,
            final ReservationRequestForm reservationRequestForm,
            final Locale locale) {
        final Match match =
                matchService
                        .findMatchById(eventId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        final List<User> confirmedParticipants = matchService.findConfirmedParticipants(eventId);
        final ModelAndView mav = new ModelAndView("matches/detail");
        mav.addObject("shell", ShellViewModelFactory.browseShell(messageSource, locale));
        mav.addObject("eventPage", buildRealEventPage(match, confirmedParticipants, locale));
        mav.addObject("reservationEnabled", match.getAvailableSpots() > 0);
        mav.addObject("reservationRequestPath", "/matches/" + eventId + "/reservations");
        mav.addObject("reservationError", reservationError);
        mav.addObject("reservationConfirmed", "confirmed".equalsIgnoreCase(reservationStatus));
        if (reservationRequestForm != null) {
            mav.addObject("reservationRequestForm", reservationRequestForm);
        }
        return mav;
    }

    private EventDetailPageViewModel buildRealEventPage(
            final Match match, final List<User> confirmedParticipants, final Locale locale) {
        return new EventDetailPageViewModel(
                toCard(match, locale),
                null,
                null,
                userService
                        .findById(match.getHostUserId())
                        .map(User::getUsername)
                        .orElse(
                                messageSource.getMessage(
                                        "event.detail.unknownHost",
                                        new Object[] {match.getHostUserId()},
                                        locale)),
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
                                        avatarLabelForUsername(participant.getUsername())))
                .toList();
    }

    private List<EventCardViewModel> loadNearbyMatches(
            final Long currentMatchId, final Locale locale) {
        final PaginatedResult<Match> result =
                matchService.searchPublicMatches(
                        "", null, "all", "soonest", 1, 4, null, null, null);
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
}
