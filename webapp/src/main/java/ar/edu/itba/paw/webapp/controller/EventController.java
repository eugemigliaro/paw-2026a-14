package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import ar.edu.itba.paw.services.exceptions.MatchReservationException;
import ar.edu.itba.paw.webapp.utils.SecurityControllerUtils;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EventController {
    private static final int DEFAULT_MAP_ZOOM = 14;

    private final MatchService matchService;
    private final MatchReservationService matchReservationService;
    private final MatchParticipationService matchParticipationService;
    private final EventPageSupport eventPageSupport;

    @Autowired
    public EventController(
            final MatchService matchService,
            final MatchReservationService matchReservationService,
            final MatchParticipationService matchParticipationService,
            final PlayerReviewService playerReviewService,
            final MessageSource messageSource,
            final Clock clock,
            @Value("${map.picker.enabled:false}") final boolean mapPickerEnabled,
            @Value("${map.tiles.urlTemplate:}") final String mapTileUrlTemplate,
            @Value("${map.tiles.attribution:}") final String mapAttribution,
            @Value("${map.default.zoom:" + DEFAULT_MAP_ZOOM + "}") final int mapDefaultZoom) {
        this.matchService = matchService;
        this.matchReservationService = matchReservationService;
        this.matchParticipationService = matchParticipationService;
        this.eventPageSupport =
                new EventPageSupport(
                        matchService,
                        matchReservationService,
                        matchParticipationService,
                        playerReviewService,
                        messageSource,
                        clock,
                        mapPickerEnabled,
                        mapTileUrlTemplate,
                        mapAttribution,
                        mapDefaultZoom);
    }

    @GetMapping("/matches/{eventId:\\d+}")
    public ModelAndView showEventDetails(
            @PathVariable("eventId") final Long eventId,
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
            @RequestParam(value = "seriesPage", defaultValue = "1") final int seriesPage,
            final Model model,
            final Locale locale) {
        return eventPageSupport.showEventDetails(
                eventId,
                flashString(model, "reservationStatus").orElse(reservationStatus),
                reservationError,
                seriesReservationErrorCode,
                flashString(model, "hostAction").orElse(hostAction),
                flashString(model, "hostActionError").orElse(null),
                flashString(model, "hostActionTarget").orElse(null),
                flashString(model, "hostInviteEmail").orElse(""),
                flashString(model, "joinStatus").orElse(joinStatus),
                joinErrorCode,
                flashString(model, "inviteStatus").orElse(inviteStatus),
                inviteErrorCode,
                Boolean.TRUE.equals(model.asMap().get("joinRequested")),
                Boolean.TRUE.equals(model.asMap().get("seriesJoinRequested")),
                seriesPage,
                locale);
    }

    @PostMapping("/matches/{matchId:\\d+}/reservations")
    public ModelAndView requestReservation(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchReservationService.reserveSpot(matchId, currentUser);
            redirectAttributes.addFlashAttribute("reservationStatus", "confirmed");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return reservationErrorDetails(matchId, exception.getCode(), locale);
        }
    }

    @PostMapping("/matches/{matchId:\\d+}/reservations/cancel")
    public ModelAndView cancelReservation(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();
        final Match cancellationContext = matchService.findMatchById(matchId).orElse(null);

        try {
            matchParticipationService.removeParticipant(matchId, currentUser, currentUser);
            if (eventPageSupport.shouldRedirectToPlayerMatchesAfterCancellation(
                    cancellationContext, currentUser)) {
                return new ModelAndView("redirect:/events");
            }
            redirectAttributes.addFlashAttribute("reservationStatus", "cancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationException exception) {
            return reservationErrorDetails(matchId, exception.getCode(), locale);
        }
    }

    @PostMapping({
        "/matches/{matchId:\\d+}/recurring-reservations",
        "/matches/{matchId:\\d+}/series-reservations"
    })
    public ModelAndView requestSeriesReservation(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchReservationService.reserveSeries(matchId, currentUser);
            redirectAttributes.addFlashAttribute("reservationStatus", "recurringConfirmed");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return seriesReservationErrorDetails(matchId, exception.getCode(), locale);
        }
    }

    @PostMapping({
        "/matches/{matchId:\\d+}/recurring-reservations/cancel",
        "/matches/{matchId:\\d+}/series-reservations/cancel"
    })
    public ModelAndView cancelSeriesReservations(
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final User currentUser = SecurityControllerUtils.requireAuthenticatedUser();

        try {
            matchReservationService.cancelSeriesReservations(matchId, currentUser);
            redirectAttributes.addFlashAttribute("reservationStatus", "recurringCancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchReservationException exception) {
            return seriesReservationErrorDetails(matchId, exception.getCode(), locale);
        }
    }

    private static Optional<String> flashString(final Model model, final String name) {
        final Object value = model.asMap().get(name);
        return value instanceof String ? Optional.of((String) value) : Optional.empty();
    }

    private ModelAndView reservationErrorDetails(
            final Long matchId, final String errorCode, final Locale locale) {
        return eventPageSupport.showEventDetails(
                matchId, null, errorCode, null, null, null, null, "", null, null, null, null, false,
                false, 1, locale);
    }

    private ModelAndView seriesReservationErrorDetails(
            final Long matchId, final String errorCode, final Locale locale) {
        return eventPageSupport.showEventDetails(
                matchId, null, null, errorCode, null, null, null, "", null, null, null, null, false,
                false, 1, locale);
    }
}
