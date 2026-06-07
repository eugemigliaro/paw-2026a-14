package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationAlreadyPendingException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationInvalidUserException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationIsHostException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNoInvitationException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNoPendingRequestException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotFoundException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotInviteOnlyException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationNotRecurringException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyJoinedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesAlreadyPendingException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesClosedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesFullException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationSeriesStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationStartedException;
import ar.edu.itba.paw.services.exceptions.matchParticipation.MatchParticipationUnauthenticatedException;
import ar.edu.itba.paw.webapp.security.annotation.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlayerParticipationController {

    private final MatchParticipationService matchParticipationService;

    @Autowired
    public PlayerParticipationController(
            final MatchParticipationService matchParticipationService) {
        this.matchParticipationService = matchParticipationService;
    }

    @PostMapping("/matches/{matchId}/join-requests")
    public ModelAndView requestToJoin(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        String errorCode = null;

        try {
            matchParticipationService.requestToJoin(matchId, user);
            redirectAttributes.addFlashAttribute("joinRequested", true);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (
                final MatchParticipationUnauthenticatedException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "unauthenticated";
        } catch (final MatchParticipationIsHostException e) {
            errorCode = "is_host";
        } catch (final MatchParticipationClosedException e) {
            errorCode = "closed";
        } catch (final MatchParticipationNotInviteOnlyException e) {
            errorCode = "not_invite_only";
        } catch (final MatchParticipationStartedException e) {
            errorCode = "started";
        } catch (final MatchParticipationAlreadyJoinedException e) {
            errorCode = "already_joined";
        } catch (final MatchParticipationAlreadyPendingException e) {
            errorCode = "already_pending";
        } catch (final MatchParticipationFullException e) {
            errorCode = "full";
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + errorCode);
    }

    @PostMapping({
        "/matches/{matchId}/recurring-join-requests",
        "/matches/{matchId}/series-join-requests"
    })
    public ModelAndView requestToJoinSeries(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        String errorCode = null;

        try {
            matchParticipationService.requestToJoinSeries(matchId, user);
            redirectAttributes.addFlashAttribute("seriesJoinRequested", true);
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (
                final MatchParticipationUnauthenticatedException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "unauthenticated";
        } catch (final MatchParticipationIsHostException e) {
            errorCode = "is_host";
        } catch (final MatchParticipationNotRecurringException e) {
            errorCode = "not_recurring";
        } catch (final MatchParticipationSeriesStartedException e) {
            errorCode = "series_started";
        } catch (final MatchParticipationSeriesClosedException e) {
            errorCode = "series_closed";
        } catch (final MatchParticipationSeriesAlreadyJoinedException e) {
            errorCode = "series_already_joined";
        } catch (final MatchParticipationSeriesAlreadyPendingException e) {
            errorCode = "series_already_pending";
        } catch (final MatchParticipationSeriesFullException e) {
            errorCode = "series_full";
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + errorCode);
    }

    @PostMapping("/matches/{matchId}/join-requests/cancel")
    public ModelAndView cancelJoinRequest(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        String errorCode = null;

        try {
            matchParticipationService.cancelJoinRequest(matchId, user);
            redirectAttributes.addFlashAttribute("joinStatus", "cancelled");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationNoPendingRequestException e) {
            errorCode = "no_pending_request";
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?joinError=" + errorCode);
    }

    @PostMapping("/matches/{matchId}/invites/accept")
    public ModelAndView acceptInvite(
            @AuthenticatedUser final User user,
            @PathVariable("matchId") final Long matchId,
            final RedirectAttributes redirectAttributes) {
        String errorCode = null;

        try {
            matchParticipationService.acceptInvite(matchId, user);
            redirectAttributes.addFlashAttribute("inviteStatus", "accepted");
            return new ModelAndView("redirect:/matches/" + matchId);
        } catch (final MatchParticipationNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (final MatchParticipationInvalidUserException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (
                final MatchParticipationIsHostException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "is_host";
        } catch (final MatchParticipationNoInvitationException e) {
            errorCode = "no_invitation";
        } catch (final MatchParticipationClosedException e) {
            errorCode = "closed";
        } catch (final MatchParticipationStartedException e) {
            errorCode = "started";
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?inviteError=" + errorCode);
    }

    @PostMapping("/matches/{matchId}/invites/decline")
    public ModelAndView declineInvite(
            @AuthenticatedUser final User user, @PathVariable("matchId") final Long matchId) {
        String errorCode = null;

        try {
            matchParticipationService.declineInvite(matchId, user);
            return new ModelAndView("redirect:/events");
        } catch (final MatchParticipationNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (final MatchParticipationInvalidUserException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } catch (
                final MatchParticipationNoInvitationException
                        e) { // TODO: move message code to service (?) and catch generic exception
            // here
            errorCode = "no_invitation";
        }
        return new ModelAndView("redirect:/matches/" + matchId + "?inviteError=" + errorCode);
    }
}
