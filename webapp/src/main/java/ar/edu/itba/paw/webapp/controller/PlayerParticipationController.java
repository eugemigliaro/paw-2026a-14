package ar.edu.itba.paw.webapp.controller;

import static ar.edu.itba.paw.webapp.utils.SecurityControllerUtils.requireAuthenticatedUserId;

import ar.edu.itba.paw.services.MatchParticipationService;
import ar.edu.itba.paw.services.exceptions.MatchParticipationException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@Controller
@PreAuthorize("isAuthenticated()")
public class PlayerParticipationController {

    private final MatchParticipationService matchParticipationService;

    @Autowired
    public PlayerParticipationController(
            final MatchParticipationService matchParticipationService) {
        this.matchParticipationService = Objects.requireNonNull(matchParticipationService);
    }

    @PostMapping("/matches/{matchId}/join-requests")
    public ModelAndView requestToJoin(@PathVariable("matchId") final String matchId) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.requestToJoin(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?join=requested");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping({
        "/matches/{matchId}/recurring-join-requests",
        "/matches/{matchId}/series-join-requests"
    })
    public ModelAndView requestToJoinSeries(@PathVariable("matchId") final String matchId) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.requestToJoinSeries(resolvedMatchId, userId);
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?join=recurringRequested");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/join-requests/cancel")
    public ModelAndView cancelJoinRequest(@PathVariable("matchId") final String matchId) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.cancelJoinRequest(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?join=cancelled");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?joinError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/invites/accept")
    public ModelAndView acceptInvite(@PathVariable("matchId") final String matchId) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.acceptInvite(resolvedMatchId, userId);
            return new ModelAndView("redirect:/matches/" + resolvedMatchId + "?invite=accepted");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?inviteError=" + e.getCode());
        }
    }

    @PostMapping("/matches/{matchId}/invites/decline")
    public ModelAndView declineInvite(@PathVariable("matchId") final String matchId) {
        final long userId = requireAuthenticatedUserId();
        final long resolvedMatchId = parseMatchIdOrThrow(matchId);

        try {
            matchParticipationService.declineInvite(resolvedMatchId, userId);
            return new ModelAndView("redirect:/events");
        } catch (final MatchParticipationException e) {
            return new ModelAndView(
                    "redirect:/matches/" + resolvedMatchId + "?inviteError=" + e.getCode());
        }
    }

    private static long parseMatchIdOrThrow(final String raw) {
        try {
            return Long.parseLong(raw);
        } catch (final NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
