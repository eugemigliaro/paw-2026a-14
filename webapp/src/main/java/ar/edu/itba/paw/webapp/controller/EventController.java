package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.Match;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.models.exceptions.match.MatchException;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.MatchReservationService;
import ar.edu.itba.paw.services.MatchService;
import ar.edu.itba.paw.services.ModerationService;
import ar.edu.itba.paw.services.PlayerReviewService;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import ar.edu.itba.paw.webapp.security.annotation.CurrentUser;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
            final ModerationService moderationService,
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
                        matchParticipationService,
                        playerReviewService,
                        moderationService,
                        clock,
                        mapPickerEnabled,
                        mapTileUrlTemplate,
                        mapAttribution,
                        mapDefaultZoom);
    }

    @GetMapping("/matches/{eventId:\\d+}")
    public ModelAndView showEventDetails(
            @CurrentUser final User user,
            @PathVariable("eventId") final Long eventId,
            @RequestParam(value = "reservation", required = false)
                    final ReservationStatus reservationStatus,
            @RequestParam(value = "reservationError", required = false)
                    final String reservationError,
            @RequestParam(value = "seriesReservationError", required = false)
                    final String seriesReservationErrorCode,
            @RequestParam(value = "hostAction", required = false) final HostAction hostAction,
            @RequestParam(value = "join", required = false) final JoinStatus joinStatus,
            @RequestParam(value = "joinError", required = false) final String joinErrorCode,
            @RequestParam(value = "invite", required = false) final InviteStatus inviteStatus,
            @RequestParam(value = "inviteError", required = false) final String inviteErrorCode,
            @RequestParam(value = "seriesPage", defaultValue = "1") final int seriesPage,
            final Model model,
            final Locale locale) {
        return eventPageSupport.showEventDetails(
                user,
                eventId,
                flashAttribute(model, "reservationStatus", ReservationStatus.class)
                        .orElse(reservationStatus),
                reservationError,
                seriesReservationErrorCode,
                flashAttribute(model, "hostAction", HostAction.class).orElse(hostAction),
                flashAttribute(model, "hostActionErrorCode", String.class).orElse(null),
                flashAttribute(model, "hostActionTarget", HostActionTarget.class).orElse(null),
                flashAttribute(model, "hostInviteEmail", String.class).orElse(""),
                flashAttribute(model, "joinStatus", JoinStatus.class).orElse(joinStatus),
                joinErrorCode,
                flashAttribute(model, "inviteStatus", InviteStatus.class).orElse(inviteStatus),
                inviteErrorCode,
                Boolean.TRUE.equals(model.asMap().get("joinRequested")),
                Boolean.TRUE.equals(model.asMap().get("seriesJoinRequested")),
                seriesPage,
                locale);
    }

    @PostMapping("/matches/{matchId:\\d+}/reservations")
    public ModelAndView requestReservation(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {

        try {
            matchReservationService.reserveSpot(matchId, user);
            redirectAttributes.addFlashAttribute("reservationStatus", ReservationStatus.CONFIRMED);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchException e) {
            return reservationErrorDetails(user, matchId, e.getMessage(), locale);
        }
    }

    @PostMapping("/matches/{matchId:\\d+}/reservations/cancel")
    public ModelAndView cancelReservation(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {
        final Match cancellationContext = matchService.findMatchById(matchId).orElse(null);

        try {
            matchParticipationService.leaveMatch(matchId, user);
            if (eventPageSupport.shouldRedirectToPlayerMatchesAfterCancellation(
                    cancellationContext, user)) {
                return new ModelAndView("redirect:/matches");
            }
            redirectAttributes.addFlashAttribute("reservationStatus", ReservationStatus.CANCELLED);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchException e) {
            return reservationErrorDetails(user, matchId, e.getMessage(), locale);
        }
    }

    @PostMapping({
        "/matches/{matchId:\\d+}/recurring-reservations",
        "/matches/{matchId:\\d+}/series-reservations"
    })
    public ModelAndView requestSeriesReservation(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {

        try {
            matchReservationService.reserveSeries(matchId, user);
            redirectAttributes.addFlashAttribute(
                    "reservationStatus", ReservationStatus.RECURRING_CONFIRMED);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchException e) {
            return seriesReservationErrorDetails(user, matchId, e.getMessage(), locale);
        }
    }

    @PostMapping({
        "/matches/{matchId:\\d+}/recurring-reservations/cancel",
        "/matches/{matchId:\\d+}/series-reservations/cancel"
    })
    public ModelAndView cancelSeriesReservations(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes,
            final Locale locale) {

        try {
            matchReservationService.cancelSeriesReservations(matchId, user);
            redirectAttributes.addFlashAttribute(
                    "reservationStatus", ReservationStatus.RECURRING_CANCELLED);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchException e) {
            return seriesReservationErrorDetails(user, matchId, e.getMessage(), locale);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> flashAttribute(
            final Model model, final String name, final Class<T> type) {
        final Object value = model.asMap().get(name);
        return type.isInstance(value) ? Optional.of((T) value) : Optional.empty();
    }

    private ModelAndView reservationErrorDetails(
            final User currentUser,
            final Long matchId,
            final String errorCode,
            final Locale locale) {
        return eventPageSupport.showEventDetails(
                currentUser,
                matchId,
                null,
                errorCode,
                null,
                null,
                null,
                null,
                "",
                null,
                null,
                null,
                null,
                false,
                false,
                1,
                locale);
    }

    private ModelAndView seriesReservationErrorDetails(
            final User currentUser,
            final Long matchId,
            final String errorCode,
            final Locale locale) {
        return eventPageSupport.showEventDetails(
                currentUser,
                matchId,
                null,
                null,
                errorCode,
                null,
                null,
                null,
                "",
                null,
                null,
                null,
                null,
                false,
                false,
                1,
                locale);
    }
}
